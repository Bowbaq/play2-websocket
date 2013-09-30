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

import com.originate.play.websocket.plugins.WebSocketHooksPlugin
import com.originate.play.websocket.{WebSocketSender, ClientConnection}
import play.api.db.slick.Config.driver.simple._
import Database.threadLocalSession
import play.api.Logger

class WebSocketHooksPluginImpl(val app: play.Application)
    extends WebSocketHooksPlugin
    with DatabaseAccess {
  def messageReceivedHook: (ClientConnection, String) => Unit = {
    (connection: ClientConnection, message: String) =>
      Logger.info(s"WebSocketHooksPlugin: message received: $connection message=$message")

      database withSession {
        val q = Query(ConnectionInfos) filter (_.connectionId === connection.connectionId) map (_.lastRequestTimestamp)
        q.update(System.currentTimeMillis())
      }

      database withSession {
        val q = Query(ConnectionInfos) filter (_.connectionId =!= connection.connectionId)

        q.list.toSet foreach {
          connectionInfo: ConnectionInfo =>
            WebSocketSender.send(connectionInfo.connectionId, s"-> $message")
        }
      }

  }

  def messageSentHook: (ClientConnection, String) => Unit = {
    (connection: ClientConnection, message: String) =>
      Logger.info(s"WebSocketHooksPlugin: message sent: $connection message=$message")
  }
}
