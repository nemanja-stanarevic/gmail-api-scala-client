org.scalastyle.sbt.ScalastylePlugin.Settings

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <url>https://github.com/nemanja-stanarevic/gmail-api-scala-client</url>
  <licenses>
    <license>
      <name>GNU AFFERO</name>
      <url>http://www.gnu.org/licenses/agpl-3.0.html</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:nemanja-stanarevic/gmail-api-scala-client.git</url>
    <connection>scm:git:git@github.com:nemanja-stanarevic/gmail-api-scala-client.git</connection>
  </scm>
  <developers>
    <developer>
      <id>nemanja-stanarevic</id>
      <name>Nemanja Stanarevic</name>
    </developer>
  </developers>)

//javaOptions += "-XX:+UnlockCommercialFeatures"

//javaOptions += "-XX:+FlightRecorder"

//javaOptions += "-XX:StartFlightRecording=duration=60s,filename=/var/tmp/testrecording.jfr,settings=profile"

//javaHome := Some(file("/Library/Java/JavaVirtualMachines/jdk1.7.0_65.jdk/Contents/Home"))

//fork := true
