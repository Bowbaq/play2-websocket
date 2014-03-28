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
import com.originate.play.websocket.{WebSocketSender, ComponentRegistry, ClientConnection}
import java.util.concurrent.TimeUnit
import play.api.Logger
import play.api.libs.iteratee.Enumerator
import scala.concurrent.duration._

class WebSocketHooksPluginImpl(val app: play.Application)
    extends WebSocketHooksPlugin {
  val connectPacketData = Connect().serialize()
  val heartbeatPacketData = Heartbeat.serialize()

  def messageReceivedHook: (ClientConnection, String) => Unit = {
    (connection: ClientConnection, message: String) =>
      Logger.debug(s"WebSocketHooksPlugin: packet received: $connection packet=$message")
      try {
        val packet = SocketIoPacket(message)
        Logger.debug(s"Received: $packet")
        if (packet.isAckRequested) {
          ComponentRegistry.main.socketIoPacketSender.send(connection.connectionId, Ack(packet.messageId))
        }

        packet match {
          case _: Disconnect =>
            WebSocketSender.disconnect(connection.connectionId)
          case _ =>
            val receive = ComponentRegistry.main.socketIoHooks.packetReceivedHook
            receive(connection, packet)
        }

      } catch {
        case e: Throwable =>
          Logger.error(s"WebSocketHooksPlugin: failed to parse packet=$message", e)
      }
  }

  def messageSentHook: (ClientConnection, String) => Unit = {
    (connection: ClientConnection, message: String) =>
      SocketIoPacket(message) match {
        case Heartbeat => scheduleOneHeartbeat(connection)
        case _ => // do nothing
      }

  }

  override def connectionEstablishedHook: (ClientConnection, Enumerator[String]) => Enumerator[String] = {
    (connection, outEnumerator) =>
      scheduleOneHeartbeat(connection)
      Enumerator(connectPacketData) >>> outEnumerator
  }

  def scheduleOneHeartbeat(connection: ClientConnection) {
    val heartbeatInterval = ComponentRegistry.main.socketIoConfig.getDuration("heartbeat.interval") getOrElse {
      Logger.warn("Cannot find 'heartbeat.interval' parameter in socketio config, using 30 sec")
      30.seconds
    }
    val shorterHeartbeatInterval = Duration.create(Math.round(heartbeatInterval.toMillis * .8), TimeUnit.MILLISECONDS)
    WebSocketSender.scheduleOnce(shorterHeartbeatInterval, connection.connectionId, heartbeatPacketData)
  }
}
