scalaVersion := "3.3.1"
Test / fork  := true

libraryDependencies ++= Seq(
  "dev.zio"       %% "zio"              % "2.1.6",
  "dev.zio"       %% "zio-json"         % "0.6.2",
  "dev.zio"       %% "zio-http"         % "3.0.0",
  "io.getquill"   %% "quill-zio"        % "4.8.5",
  "io.getquill"   %% "quill-jdbc-zio"   % "4.8.4",
  "com.h2database" % "h2"               % "2.2.224",
  "dev.zio"       %% "zio-test"         % "2.1.4"     % Test,
  "dev.zio"       %% "zio-http-testkit" % "3.0.0"     % Test,
  "dev.zio"       %% "zio-test-sbt"     % "2.1.6"     % Test
)

resolvers ++= Resolver.sonatypeOssRepos("snapshots")
