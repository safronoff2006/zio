
// The simplest possible sbt build file is just one line:


 lazy val root = (project in file(".")).
   settings(
     inThisBuild(List(
       organization := "safronoff2006",
       scalaVersion := "2.13.14",
       version := "1.0"
     )),
     name := "zio-http.quill.json",
     Compile / run / mainClass := Option("ru.lesson.MainApp"),
     testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
     libraryDependencies ++= Seq(
       "org.scala-lang.modules" %% "scala-parser-combinators" % "2.3.0",
       "dev.zio" %% "zio" % "2.1.9",
       "dev.zio" %% "zio-streams" % "2.1.9",
       "dev.zio" %% "zio-http" % "3.0.0-RC9",
       "dev.zio" %% "zio-json" % "0.7.3",
       "dev.zio" %% "zio-test"          % "2.1.9" % Test,
       "dev.zio" %% "zio-test-sbt"      % "2.1.9" % Test,
       "dev.zio" %% "zio-test-magnolia" % "2.1.9" % Test,
       "io.getquill"          %% "quill-jdbc-zio" % "4.8.4",
       "org.postgresql"       %  "postgresql"     % "42.7.3",
       "ch.qos.logback" % "logback-classic" % "1.5.6"




     )
   )


