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

import play.api._

import play.api.db.DB
import play.api.GlobalSettings
import plugins.ConnectionInfos
import scala.slick.session.Database
import Database.threadLocalSession

import play.api.db.slick.Config.driver.simple._
import play.api.Play.current

object Global extends GlobalSettings {
  override def onStart(app: Application) {
    lazy val database = Database.forDataSource(DB.getDataSource())

    try {
      database.withSession {
        ConnectionInfos.ddl.create
      }
    } catch {
      case e: Throwable => Logger.error(s"Error during ConnectionInfos.ddl.create: ${e.getMessage}")
    }
  }
}