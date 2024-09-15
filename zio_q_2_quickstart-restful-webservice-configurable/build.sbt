scalaVersion := "2.13.14"

libraryDependencies ++= Seq(
  "dev.zio"       %% "zio"                 % "2.1.4",
  "dev.zio"       %% "zio-json"            % "0.7.3",
  "dev.zio"       %% "zio-http"            % "3.0.0",
  "io.getquill"   %% "quill-zio"           % "4.8.4",
  "io.getquill"   %% "quill-jdbc-zio"      % "4.8.4",
  "com.h2database" % "h2"                  % "2.2.224",
  "dev.zio"       %% "zio-config"          % "4.0.2",
  "dev.zio"       %% "zio-config-typesafe" % "4.0.2",
  "dev.zio"       %% "zio-config-magnolia" % "4.0.2"
)

resolvers ++= Resolver.sonatypeOssRepos("snapshots")
