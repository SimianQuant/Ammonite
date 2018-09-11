package ammonite.kernel

import collection.mutable
import scala.tools.nsc.{Global => G}
import scalaz.{Name => _, _}
import Scalaz._
import Validation.FlatMap._
import kernel.{generatedMain, newLine}

private[kernel] final case class MungedOutput(code: String, prefixCharLength: Int)

/** Munges input statements into a form that can be fed into scalac
  */
private[kernel] object Munger {

  private case class Transform(code: String, resIden: Option[String])

  private type DCT = (String, String, G#Tree) => Option[Transform]

  def apply(
      stmts: NonEmptyList[String],
      resultIndex: String,
      pkgName: Seq[Name],
      indexedWrapperName: Name,
      imports: Imports,
      parse: => String => ValidationNel[LogError, Seq[G#Tree]]): ValidationNel[LogError, MungedOutput] = {

    // type signatures are added below for documentation

    val decls: List[DCT] = {

      def defProc(cond: PartialFunction[G#Tree, G#Name]): DCT =
        (code: String, name: String, tree: G#Tree) =>
          cond.lift(tree).map { name =>
            Transform(code, None)
        }

      def processor(cond: PartialFunction[(String, String, G#Tree), Transform]): DCT = {
        (code: String, name: String, tree: G#Tree) =>
          cond.lift((name, code, tree))
      }

      val objectDef = defProc {
        case m: G#ModuleDef => m.name
      }

      val classDef = defProc {
        case m: G#ClassDef if !m.mods.isTrait => m.name
      }

      val traitDef = defProc {
        case m: G#ClassDef if m.mods.isTrait => m.name
      }

      val defDef = defProc {
        case m: G#DefDef => m.name
      }

      val typeDef = defProc {
        case m: G#TypeDef => m.name
      }

      val patVarDef = processor {
        case (name, code, t: G#ValDef) => Transform(code, None)
      }

      val importDef = processor {
        case (name, code, tree: G#Import) => Transform(code, None)
      }

      val expr = processor {
        //Expressions are lifted to anon function applications so they will be JITed
        case (name, code, tree) =>
          Transform(s"private val $name = $code", Some(name))
      }

      List(
        objectDef,
        classDef,
        traitDef,
        defDef,
        typeDef,
        patVarDef,
        importDef,
        expr
      )
    }

    val composed: String => ValidationNel[LogError, (Seq[G#Tree], String)] =
      x => parse(x) map (y => (y, x))

    val parsed: ValidationNel[LogError, NonEmptyList[(Seq[G#Tree], String)]] =
      stmts.traverseU(composed)

    def declParser(inp: ((Seq[G#Tree], String), Int)): ValidationNel[LogError, Transform] =
      inp match {
        case ((trees, code), i) =>
          def handleTree(t: G#Tree): ValidationNel[LogError, Transform] = {
            val parsedDecls: List[Transform] = decls flatMap (x => x(code, "res" + resultIndex + "_" + i, t))
            parsedDecls match {
              case h :: t => Success(h)
              case Nil =>
                Failure(NonEmptyList(LogError(s"Dont know how to handle $code")))
            }
          }
          trees match {
            case Seq(h) => handleTree(h)
            case _ if trees.nonEmpty && trees.forall(_.isInstanceOf[G#Import]) =>
              handleTree(trees.head)
            case _ =>
              val filteredSeq = trees filter (_.isInstanceOf[G#ValDef])
              filteredSeq.toList.traverseU(handleTree).map { transforms =>
                transforms.lastOption match {
                  case Some(Transform(_, resIden)) => Transform(code, resIden)
                  case None => Transform(code, None)
                }
              }
          }
      }

    val declTraversed: ValidationNel[LogError, NonEmptyList[Transform]] =
      parsed.map(_.zipWithIndex).flatMap(_.traverseU(declParser))

    val expandedCode: ValidationNel[LogError, Transform] = declTraversed map {
      case NonEmptyList(h, tl) =>
        tl.foldLeft(h) {
          case (acc, v) => Transform(acc.code ++ v.code, v.resIden)
        }
    }

    expandedCode map {
      case Transform(code, resIden) =>
        // can't use strip Margin below because holier-than-thou libraries like shapeless and scalaz use weird
        // characters for identifiers

        val topWrapper = s"""
           package ${pkgName.map(_.backticked).mkString(".")}
           ${importBlock(imports)}
           object ${indexedWrapperName.backticked}{\n"""

        val previousIden = resIden.getOrElse("()")

        val bottomWrapper = s"""
          def $generatedMain = { $previousIden }
          }"""

        val importsLen = topWrapper.length

        MungedOutput(topWrapper + code + bottomWrapper, importsLen)
    }

  }

  def importBlock(importData: Imports): String = {
    // Group the remaining imports into sliding groups according to their
    // prefix, while still maintaining their ordering
    val grouped = mutable.Buffer[mutable.Buffer[ImportData]]()
    for (data <- importData.value) {
      if (grouped.isEmpty) {
        grouped.append(mutable.Buffer(data))
      } else {
        val last = grouped.last.last

        // Start a new import if we're importing from somewhere else, or
        // we're importing the same thing from the same place but aliasing
        // it to a different name, since you can't import the same thing
        // twice in a single import statement
        val startNewImport =
          last.prefix != data.prefix || grouped.last.exists(_.fromName == data.fromName)

        if (startNewImport) {
          grouped.append(mutable.Buffer(data))
        } else {
          grouped.last.append(data)
        }
      }
    }
    // Stringify everything
    val out = for (group <- grouped) yield {
      val printedGroup = for (item <- group) yield {
        if (item.fromName == item.toName) {
          item.fromName.backticked
        } else {
          s"${item.fromName.backticked} => ${item.toName.backticked}"
        }
      }
      val pkgString = group.head.prefix.map(_.backticked).mkString(".")
      "import " + pkgString + s".{$newLine  " +
        printedGroup.mkString(s",$newLine  ") + s"$newLine}$newLine"
    }
    out.mkString
  }

}
