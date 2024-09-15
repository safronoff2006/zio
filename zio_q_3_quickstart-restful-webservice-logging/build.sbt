scalaVersion := "2.13.14"

libraryDependencies ++= Seq(
  "dev.zio"       %% "zio"                 % "2.1.4",
  "dev.zio"       %% "zio-json"            % "0.7.3",
  "dev.zio"       %% "zio-http"            % "3.0.0",
  "io.getquill"   %% "quill-zio"           % "4.8.4",
  "io.getquill"   %% "quill-jdbc-zio"      % "4.8.4",
  "com.h2database" % "h2"                  % "2.2.224",
  "dev.zio"       %% "zio-logging"       % "2.3.0",
  "dev.zio"       %% "zio-logging-slf4j" % "2.2.4",
  "org.slf4j"      % "slf4j-simple"      % "2.0.13"
)

resolvers ++= Resolver.sonatypeOssRepos("snapshots")
