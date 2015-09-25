

scalaVersion in ThisBuild := "2.11.6"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.9"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.3.9"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.1"

lazy val root = (project in file(".")).
settings(
    name := "timeout",
    version := "0.1"
)