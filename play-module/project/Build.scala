/*
   Copyright 2013 Originate Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
import sbt._
import Keys._

object ApplicationBuild extends Build {

  val appName         = "play2-websocket"
  val appVersion      = "1.0.1-SNAPSHOT"

  val appDependencies = Seq(
    "com.typesafe.akka" %% "akka-remote" % "2.1.2"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    organization := "com.originate",
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature"),
    publishTo <<= version { (v: String) =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    publishMavenStyle := true,
    pomIncludeRepository := { _ => false },
    pomExtra := (
  <url>https://github.com/Originate/play2-websocket</url>
  <licenses>
    <name>The Apache Software License, Version 2.0</name>
    <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
    <distribution>repo</distribution>
  </licenses>
  <scm>
    <url>git@github.com:Originate/play2-websocket.git</url>
    <connection>scm:git@github.com:Originate/play2-websocket.git</connection>
  </scm>
  <developers>
    <developer>
      <id>dtarima</id>
      <name>Denis Tarima</name>
      <url>http://originate.com</url>
    </developer>
  </developers>)
  )

}
