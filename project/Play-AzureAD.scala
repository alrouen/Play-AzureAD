import sbt._
import sbt.Keys._

object BuildSettings {
  val buildVersion = "0.0.0-SNAPSHOT"

  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "org.freesp",
    version := buildVersion,
    scalaVersion := "2.11.6",
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-target:jvm-1.8"),
    crossScalaVersions := Seq("2.11.6"),
    crossVersion := CrossVersion.binary
  )
}

object PlayAzureADBuild extends Build {
  import BuildSettings._

  lazy val azureAd = Project(
    "Play-AzureAD",
    file("."),
    settings = buildSettings ++ Seq(
      resolvers := Seq(
        "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
        "Sonatype" at "http://oss.sonatype.org/content/groups/public/",
        "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/",
        "Typesafe repository snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"
      ),
      libraryDependencies ++= Seq(
        "com.typesafe.play" %% "play" % "2.4.0" % "provided" cross CrossVersion.binary,
        "com.typesafe.play" %% "play-ws" % "2.4.0" % "provided" cross CrossVersion.binary,
        "com.typesafe.play" %% "play-cache" % "2.4.0" % "provided" cross CrossVersion.binary,
        "com.typesafe.play" %% "play-test" % "2.4.0" % "test" cross CrossVersion.binary,
        "org.bitbucket.b_c" % "jose4j" % "0.4.1",
        "org.specs2" % "specs2" % "2.3.12" % "test" cross CrossVersion.binary,
        "junit" % "junit" % "4.8" % "test" cross CrossVersion.Disabled,
        "org.apache.logging.log4j" % "log4j-to-slf4j" % "2.0.2"
      )
    )
  )
}