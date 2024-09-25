import sbt.Keys.scalacOptions
// The simplest possible sbt build file is just one line:


lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "safronoff2006",
      scalaVersion := "2.13.14",
      version := "1.0"
    )),
    name := "zio-http.quill.hocon_resources",
    Compile / run / mainClass := Option("ru.lesson.MainApp"),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-parser-combinators" % "2.4.0",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
      "ch.qos.logback" % "logback-classic" % "1.5.6",
      "dev.zio" %% "zio" % "2.1.9",
      "dev.zio" %% "zio-streams" % "2.1.9",
      "dev.zio" %% "zio-macros" % "2.1.9",
      "dev.zio" %% "zio-http" % "3.0.0",
      "dev.zio" %% "zio-json" % "0.7.3",
      "dev.zio" %% "zio-json-macros" % "0.7.1",
      "dev.zio" %% "zio-config"          % "4.0.2",
      "dev.zio" %% "zio-config-typesafe" % "4.0.2",
      "dev.zio" %% "zio-config-magnolia" % "4.0.2",
      "dev.zio" %% "zio-test" % "2.1.9" %  Test,
      "dev.zio" %% "zio-http-testkit" % "3.0.0" % Test,
      "dev.zio" %% "zio-test-sbt" % "2.1.9" % Test,
      "dev.zio" %% "zio-test-magnolia" % "2.1.9" % Test,
      "io.getquill" %% "quill-zio" % "4.8.4",
      "io.getquill" %% "quill-jdbc-zio" % "4.8.4",
      "org.postgresql" % "postgresql" % "42.7.3",
      "dev.zio" %% "zio-logging" % "2.3.0",
      "dev.zio" %% "zio-logging-slf4j" % "2.2.4",


    ),
    resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
    scalacOptions += "-Ymacro-annotations"
  )


