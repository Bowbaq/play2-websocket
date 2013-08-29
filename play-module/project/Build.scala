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
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "com.typesafe.akka" %% "akka-remote" % "2.1.2"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    publishTo := Some(Resolver.file("Local repository",
      // TODO(dtarima): is there a way to avoid absolute path?
      file("/home/dtarima/devel/tools/sdk/play-2.1.3/repository/local"))(Resolver.ivyStylePatterns)),
    publishMavenStyle := false,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature"),
    organization := "com.originate"
  )

}
