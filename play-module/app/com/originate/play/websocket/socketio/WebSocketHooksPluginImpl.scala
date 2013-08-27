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
package com.originate.play.websocket.socketio

import com.originate.play.websocket.plugins.WebSocketHooksPlugin
import com.originate.play.websocket.{ComponentRegistry, ClientConnection}
import play.api.Logger
import play.api.libs.iteratee.Enumerator

class WebSocketHooksPluginImpl(val app: play.Application)
    extends WebSocketHooksPlugin {
  def messageReceivedHook: (ClientConnection, String) => Unit = {
    (connection: ClientConnection, message: String) =>
      Logger.info(s"WebSocketHooksPlugin: packet received: $connection packet=$message")
      try {
        val packet = SocketIoPacket(message)
        Logger.info(s"Received: $packet")
        if (packet.isAckRequested) {
          ComponentRegistry.main.socketIoPacketSender.send(connection.connectionId, Ack(packet.messageId))
        }

        val receive = ComponentRegistry.main.socketIoHooks.packetReceivedHook
        receive(connection, packet)

      } catch {
        case e: Throwable =>
          Logger.error(s"WebSocketHooksPlugin: failed to parse packet=$message", e)
      }
  }

  val connectPacketData = Connect().serialize() // "1:::"

  override def connectionEstablishedHook: (ClientConnection, Enumerator[String]) => Enumerator[String] = {
    (connection, outEnumerator) => Enumerator(connectPacketData) >>> outEnumerator
  }
}
