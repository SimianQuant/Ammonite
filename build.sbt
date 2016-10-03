lazy val Benchmark = config("bench") extend Test

lazy val scalaMeterFramework = new TestFramework("org.scalameter.ScalaMeterFramework")

lazy val root = Project(
    "ammonite-kernel-root",
    file("."),
    settings = Defaults.coreDefaultSettings ++ Seq(
        name := "ammonite-kernel",
        organization := "com.simianquant",
        scalaVersion := "2.11.8",
        fork := true,
        version := "0.1",
        scalacOptions ++= Seq("-Ywarn-unused",
                              "-Ywarn-unused-import",
                              "-Ywarn-inaccessible",
                              "-Ywarn-dead-code",
                              "-explaintypes",
                              "-Xlog-reflective-calls",
                              "-Ywarn-value-discard",
                              "-Xlint",
                              "-deprecation",
                              "-Ywarn-nullary-override",
                              "-Ywarn-nullary-unit",
                              "-feature",
                              "-unchecked",
                              "-Xfuture"),
        resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
        libraryDependencies ++= Seq("org.scala-lang" % "scala-compiler" % scalaVersion.value,
                                    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
                                    "com.lihaoyi" %% "scalaparse" % "0.3.7",
                                    "com.lihaoyi" %% "ammonite-ops" % "0.7.6",
                                    "org.scalaz" %% "scalaz-core" % "7.2.6",
                                    "ch.qos.logback" % "logback-classic" % "1.1.7",
                                    "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0",
                                    "io.get-coursier" %% "coursier" % "1.0.0-M14",
                                    "io.get-coursier" %% "coursier-cache" % "1.0.0-M14",
                                    "org.scalatest" %% "scalatest" % "3.0.0" % "test"),
        autoCompilerPlugins := true,
        addCompilerPlugin("org.psywerx.hairyfotr" %% "linter" % "0.1.15"),
        ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) },
        javaOptions += "-Xmx4G",
        logBuffered in Test := false,
        testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oD"),
        testFrameworks += scalaMeterFramework,
        parallelExecution in Benchmark := false
      )
  ) configs (
    Benchmark
  ) settings (
    inConfig(Benchmark)(Defaults.testSettings): _*
  )
