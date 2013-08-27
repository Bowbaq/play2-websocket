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
package plugins

import play.api.Logger
import play.api.db.slick.Config.driver.simple._
import Database.threadLocalSession
import com.originate.play.websocket.{ClientInfo, ClientConnection}
import com.originate.play.websocket.plugins.ConnectionRegistrarPlugin

class ConnectionRegistrarPluginImpl(val app: play.Application)
    extends ConnectionRegistrarPlugin
    with DatabaseAccess {

  def register(connection: ClientConnection) {
    Logger.info(s"ConnectionRegistrarPlugin: register: $connection")

    database withSession {
      ConnectionInfos insert ConnectionInfo(
        connection.connectionId,
        connection.clientInfo.clientId,
        connection.clientInfo.userId,
        connection.connectionActorUrl,
        System.currentTimeMillis())
    }
  }

  def deregister(connection: ClientConnection) {
    Logger.info(s"ConnectionRegistrarPlugin: deregister: $connection")

    database withSession {
      val q = Query(ConnectionInfos) filter (_.connectionId === connection.connectionId)
      q.delete
    }
  }

  def find(connectionId: String): Option[ClientConnection] = {
    Logger.info(s"ConnectionRegistrarPlugin: find $connectionId")

    val clientConnectionOpt = database withSession {
      val q = Query(ConnectionInfos) filter (_.connectionId === connectionId)
      q.list.headOption
    } map {
      connectionInfo =>
        ClientConnection(
          ClientInfo(connectionInfo.userId, connectionInfo.clientId),
          connectionInfo.connectionId,
          connectionInfo.connectionActorUrl)
    }
    Logger.info(s"ConnectionRegistrarPlugin: found $clientConnectionOpt")
    clientConnectionOpt
  }
}
