import sbt._
import sbt.Keys._

object GmailapiscalaclientBuild extends Build {

  lazy val gmailapiscalaclient = Project(
    id = "gmail-api-scala-client",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "gmail-api-scala-client",
      organization := "com.github.nemanja-stanarevic",
      version := "0.1.0-SNAPSHOT",
      scalaVersion := "2.11.2",

      resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases",
      resolvers += "Sonatype Releases" at "http://oss.sonatype.org/content/repositories/releases",
      resolvers += "Spray Repository" at "http://repo.spray.io/",

      libraryDependencies ++= {
        val akkaVersion = "2.3.4"
        val sprayVersion = "1.3.1"
        val scalacheckVersion = "1.11.5"
        val scalaTestVersion = "2.2.1"
        val specs2Version = "2.4"
        val json4sVersion = "3.2.10"
        val mimepullVersion = "1.9.4"
        val commonsEmailVersion = "1.3.3"
        val commonsCodecVersion = "1.9"
        Seq(
          "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
          "org.scalacheck" %% "scalacheck" % scalacheckVersion % "test",
          "org.specs2" %% "specs2" % specs2Version % "test",

          "com.typesafe.akka" %% "akka-actor" % akkaVersion,
          "com.typesafe.akka" %% "akka-testkit" % akkaVersion,

          "org.apache.commons" % "commons-email" % commonsEmailVersion,
          "commons-codec" % "commons-codec" % commonsCodecVersion,

          "io.spray" %% "spray-can" % sprayVersion,
          "io.spray" %% "spray-client" % sprayVersion,
          "io.spray" %% "spray-util" % sprayVersion,
          "io.spray" %% "spray-http" % sprayVersion,
          "io.spray" %% "spray-httpx" % sprayVersion,
          "io.spray" %% "spray-testkit" % sprayVersion % "test",

          "org.jvnet.mimepull" % "mimepull" % mimepullVersion,
          "org.json4s" %% "json4s-jackson" % json4sVersion,
          "org.json4s" %% "json4s-ext" % json4sVersion)
      }))
}
