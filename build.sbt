val Http4sVersion = "0.19.0"
val Specs2Version = "4.1.0"
val LogbackVersion = "1.2.3"
val Fs2Version = "0.9.3"
val CirceVersion = "0.10.0"

lazy val root = (project in file("."))
  .settings(
    organization := "com.example",
    name := "tweettally",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.6",
    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s"      %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s"      %% "http4s-circe"        % Http4sVersion,
      "co.fs2"          %% "fs2-core"            % Fs2Version,
      "io.circe"        %% "circe-fs2"           % "0.10.0",
      "io.circe"        %% "circe-core"          % CirceVersion,
      "io.circe"        %% "circe-generic"       % CirceVersion,
      "io.circe"        %% "circe-parser"        % CirceVersion,
      "org.http4s"      %% "http4s-dsl"          % Http4sVersion,
      "org.specs2"      %% "specs2-core"         % Specs2Version % "test",
      "com.typesafe"    %  "config"              % "1.3.3",
      "org.scalatest"   %% "scalatest" % "3.0.5" % Test,
      "ch.qos.logback"  %  "logback-classic"     % LogbackVersion
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector"     % "0.9.6"),
    addCompilerPlugin("com.olegpy"     %% "better-monadic-for" % "0.2.4")
  )

