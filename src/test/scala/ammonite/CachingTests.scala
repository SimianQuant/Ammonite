// package ammonite

// import ammonite.runtime.Storage
// import ammonite.main.Defaults
// import ammonite.ops._
// import ammonite.runtime.tools.IvyConstructor._
// import ammonite.TestUtils._
// //import utest._
// import org.scalatest.FreeSpec

// class CachingTests extends FreeSpec {

//   val scriptPath = pwd / 'src / 'test / 'resources / 'scripts

//   val resourcesPath = pwd / 'src / 'test / 'resources
//   val tempDir = tmp.dir(prefix = "ammonite-tester")

//   "noAutoIncrementWrapper" in {
//     val storage = Storage.InMemory()
//     val interp = createTestInterp(storage)
//     interp.interpApi.load.module(scriptPath / "ThreeBlocks.sc")
//     try {
//       Class.forName("cmd0")
//       assert(false)
//     } catch {
//       case e: ClassNotFoundException => assert(true)
//       case e: Exception => assert(false)
//     }
//   }

//   "blocks" in {
//     val cases =
//       Seq("OneBlock.sc" -> 2, "TwoBlocks.sc" -> 3, "ThreeBlocks.sc" -> 4)
//     for ((fileName, expected) <- cases) {
//       val storage = Storage.InMemory()
//       val interp = createTestInterp(storage)
//       val n0 = storage.compileCache.size

//       assert(n0 == 1) // customLolz predef
//       interp.interpApi.load.module(scriptPath / fileName)

//       val n = storage.compileCache.size
//       assert(n == expected)
//     }
//   }

//   "processModuleCaching" - {
//     def check(script: RelPath) {
//       val storage = new Storage.Folder(tempDir)

//       val interp1 = createTestInterp(
//         storage,
//         Defaults.predefString
//       )
//       interp1.interpApi.load.module(resourcesPath / script)
//       assert(interp1.compiler != null)
//       val interp2 = createTestInterp(
//         storage,
//         Defaults.predefString
//       )
//       assert(interp2.compiler == null)
//       interp2.interpApi.load.module(resourcesPath / script)
//       assert(interp2.compiler == null)
//     }

//     "testOne" in check('scriptLevelCaching / "scriptTwo.sc")
//     "testTwo" in check('scriptLevelCaching / "scriptOne.sc")
//     "testThree" in check('scriptLevelCaching / "QuickSort.sc")
//     "testLoadModule" in check('scriptLevelCaching / "testLoadModule.sc")
//     "testFileImport" in check('scriptLevelCaching / "testFileImport.sc")
//     "testIvyImport" in check('scriptLevelCaching / "ivyCacheTest.sc")
//   }

//   "testRunTimeExceptionForCachedScripts" in {
//     val storage = new Storage.Folder(tempDir)
//     val numFile = pwd / 'target / 'test / 'resources / 'scriptLevelCaching / "num.value"
//     rm(numFile)
//     write(numFile, "0")
//     val interp1 = createTestInterp(storage, Defaults.predefString)
//     assertThrows[java.lang.ArithmeticException] {
//       interp1.interpApi.load.module(resourcesPath / 'scriptLevelCaching / "runTimeExceptions.sc")
//     }
//     val interp2 = createTestInterp(storage, Defaults.predefString)
//     assertThrows[java.lang.ArithmeticException] {
//       interp2.interpApi.load.module(resourcesPath / 'scriptLevelCaching / "runTimeExceptions.sc")
//     }
//   }

//   "persistence" in {

//     val tempDir = ammonite.ops.Path(
//       java.nio.file.Files.createTempDirectory("ammonite-tester-x")
//     )

//     val interp1 = createTestInterp(new Storage.Folder(tempDir))
//     val interp2 = createTestInterp(new Storage.Folder(tempDir))
//     interp1.interpApi.load.module(scriptPath / "OneBlock.sc")
//     interp2.interpApi.load.module(scriptPath / "OneBlock.sc")
//     val n1 = interp1.compilationCount
//     val n2 = interp2.compilationCount
//     assert(n1 == 2) // customLolz predef + OneBlock.sc
//     assert(n2 == 0) // both should be cached
//   }

//   "tags" in {
//     val storage = Storage.InMemory()
//     val interp = createTestInterp(storage)
//     interp.interpApi.load.module(scriptPath / "TagBase.sc")
//     interp.interpApi.load.module(scriptPath / "TagPrevCommand.sc")
//     interp.interpApi.load.ivy("com.lihaoyi" %% "scalatags" % "0.4.5")
//     interp.interpApi.load.module(scriptPath / "TagBase.sc")
//     val n = storage.compileCache.size
//     assert(n == 5) // customLolz predef + two blocks for each loaded file
//   }

//   "changeScriptInvalidation" in {
//     // This makes sure that the compile caches are properly utilized, and
//     // flushed, in a variety of circumstances: changes to the number of
//     // blocks in the predef, predefs containing magic imports, and changes
//     // to the script being run. For each change, the caches should be
//     // invalidated, and subsequently a single compile should be enough
//     // to re-fill the caches
//     val predefFile =
//       tmp("""
//         val x = 1337
//         @
//         val y = x
//         import $ivy.`com.lihaoyi::scalatags:0.5.4`, scalatags.Text.all._
//         """)
//     val scriptFile = tmp("""div("<('.'<)", y).render""")

//     def processAndCheckCompiler(f: ammonite.runtime.Compiler => Boolean) = {
//       val interp = createTestInterp(
//         new Storage.Folder(tempDir) {
//           override val predef = predefFile
//         },
//         Defaults.predefString
//       )
//       interp.interpApi.load.module(scriptFile)
//       assert(f(interp.compiler))
//     }

//     processAndCheckCompiler(_ != null)
//     processAndCheckCompiler(_ == null)

//     rm ! predefFile
//     write(
//       predefFile,
//       """
//         import $ivy.`com.lihaoyi::scalatags:0.5.4`; import scalatags.Text.all._
//         val y = 31337
//         """
//     )

//     processAndCheckCompiler(_ != null)
//     processAndCheckCompiler(_ == null)

//     rm ! scriptFile
//     write(
//       scriptFile,
//       """div("(>'.')>", y).render"""
//     )

//     processAndCheckCompiler(_ != null)
//     processAndCheckCompiler(_ == null)
//   }

// }