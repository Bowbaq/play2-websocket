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
package controllers

import play.api._
import play.api.mvc._
import play.api.db.DB
import java.util.UUID
import scala.slick.session.Database
import play.api.Play.current

object Application extends Controller {

  lazy val database = Database.forDataSource(DB.getDataSource())

  def index = Action {
    implicit request =>

      val session = request.session.get("s") match {
        case Some(sessionId) => {
          request.session
        }
        case _ =>
          val sessionId = UUID.randomUUID().toString
          Logger.info("Session created: " + sessionId)
          request.session +("s", sessionId)
      }

      Ok(views.html.index("Your new application is ready.")).withSession(session)
  }

}