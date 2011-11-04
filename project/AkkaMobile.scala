import sbt._
import Keys._


object MyBuild extends Build {

  import Dependencies._

  lazy val buildSettings = Seq(
    organization := "info.gamlor.akkamobile",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.9.1"
  )

  lazy val root = Project("root", file(".")) aggregate (akkamobileClient)

  lazy val akkamobileClient: Project = Project(
    id = "akka-mobile-client",
    base = file("./akka-mobile-client"),
    settings = defaultSettings ++ Seq(
      unmanagedBase <<= baseDirectory {
        base => base / "lib"
      },
      libraryDependencies ++= Seq(akkaActors, scalaTest, akkaTestKit)
    ))

  lazy val akkamobileServer: Project = Project(
    id = "akka-mobile-server",
    base = file("./akka-mobile-server"),
    dependencies = Seq(akkamobileClient),
    settings = defaultSettings ++ Seq(
      libraryDependencies ++= Seq(akkaActors, akkaRemoteActors, netty, scalaTest, akkaTestKit)
    )
  )

  lazy val akkamobileTest: Project = Project(
    id = "akka-mobile-test",
    base = file("./akka-mobile-test"),
    dependencies = Seq(akkamobileClient, akkamobileServer),
    settings = defaultSettings ++ Seq(
      libraryDependencies ++= Seq(akkaActors, akkaRemoteActors, netty, scalaTest, akkaTestKit)
    ))


  override lazy val settings = super.settings ++ buildSettings

  lazy val defaultSettings = Defaults.defaultSettings ++ Seq(
    resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",

    // compile options
    scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked") ++ (
      if (true || (System getProperty "java.runtime.version" startsWith "1.7")) Seq() else Seq("-optimize")), // -optimize fails with jdk7
    javacOptions ++= Seq("-source", "1.6", "-target", "1.6", "-Xlint:deprecation"),
    // show full stack traces
    testOptions in Test += Tests.Argument("-oF")
  )


}

object Dependencies {

  val scalaTest = "org.scalatest" %% "scalatest" % "1.6.1" % "test"
  val netty = "org.jboss.netty" % "netty" % "3.2.5.Final"

  val akkaTestKit = "se.scalablesolutions.akka" % "akka-testkit" % "1.2" % "test"
  val akkaActors = "se.scalablesolutions.akka" % "akka-actor" % "1.2"
  val akkaRemoteActors = "se.scalablesolutions.akka" % "akka-remote" % "1.2"
}
