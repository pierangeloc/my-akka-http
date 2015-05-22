enablePlugins(JavaAppPackaging)

name         := """my-akka-http"""
organization := "com.theiterators"
version      := "1.0"
scalaVersion := "2.11.5"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV       = "2.3.10"
  val akkaStreamV = "1.0-RC2"
  val scalaTestV  = "2.2.4"
  Seq(
    "com.typesafe.akka" %% "akka-actor"                           % akkaV,
    "com.typesafe.akka" %% "akka-stream-experimental"             % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-core-experimental"          % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-scala-experimental"         % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental"    % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-testkit-scala-experimental" % akkaStreamV,
    "org.reactivemongo" %% "reactivemongo"                        % "0.10.5.0.akka23",
    "org.reactivemongo" %% "play2-reactivemongo"                  % "0.10.5.0.akka23",
    "com.typesafe.play" % "play-json_2.11"                        % "2.4.0-M2",
    "ch.qos.logback"    % "logback-classic"                       % "1.1.2",
    "org.scalaz"        %% "scalaz-core" % "7.1.2",


    "org.scalatest"     %% "scalatest"                            % scalaTestV % "test"
  )
}
//initialize the Revolver plugin
Revolver.settings
