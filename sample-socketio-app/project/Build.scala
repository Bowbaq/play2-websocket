import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "sample-socketio-app"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "com.typesafe.play" %% "play-slick" % "0.3.3",
    "com.typesafe.slick" %% "slick" % "1.0.0",
    "com.typesafe.akka" %% "akka-remote" % "2.1.2",
    "mysql" % "mysql-connector-java" % "5.1.26",
    "com.originate" %% "play2-websocket" % "1.0.1-SNAPSHOT"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature"),
    organization := "com.originate"
  )

}
