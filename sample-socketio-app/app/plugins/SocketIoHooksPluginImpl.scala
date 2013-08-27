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

import com.originate.play.websocket.socketio.plugins.SocketIoHooksPlugin
import com.originate.play.websocket.socketio._
import play.api.db.slick.Config.driver.simple._
import Database.threadLocalSession
import play.api.Logger
import com.originate.play.websocket.socketio.Event
import com.originate.play.websocket.ClientConnection
import com.originate.play.websocket.socketio.Message

class SocketIoHooksPluginImpl(val app: play.Application)
    extends SocketIoHooksPlugin
    with DatabaseAccess {
  def packetReceivedHook: (ClientConnection, SocketIoPacket) => Unit = {
    (connection, packet) =>
      Logger.info(s"SocketIoHooksPlugin: received packet")
      database withSession {
        val q = Query(ConnectionInfos) filter (_.connectionId === connection.connectionId) map (_.lastRequestTimestamp)
        q.update(System.currentTimeMillis())
      }

      val packetOpt = packet match {
        case p: Message =>
          Some(p.copy(messageId = "", ack = Acknowledge.No))
        case p: JsonMessage =>
          Some(p.copy(messageId = "", ack = Acknowledge.No))
        case p: Event =>
          Some(p.copy(messageId = "", ack = Acknowledge.No))
        case _ =>
          Logger.info(s"Packet doesn't need to be sent: $packet")
          None
      }

      packetOpt map {
        packet => database withSession {
          val q = Query(ConnectionInfos) filter (_.connectionId =!= connection.connectionId)

          q.list.toSet foreach {
            connectionInfo: ConnectionInfo =>
              SocketIoSender.send(connectionInfo.connectionId, packet)
          }
        }
      }

  }
}
