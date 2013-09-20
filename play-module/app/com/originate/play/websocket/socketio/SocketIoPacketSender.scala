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

import com.originate.play.websocket.WebSocketMessageSenderComponent
import com.originate.play.websocket.plugins.ConnectionRegistrarComponent

trait SocketIoPacketSender {
  def send(connectionId: String, packet: SocketIoPacket): Unit
}

trait SocketIoPacketSenderComponent {
  val socketIoPacketSender: SocketIoPacketSender
}

trait SocketIoPacketSenderComponentImpl extends SocketIoPacketSenderComponent {
  this: ConnectionRegistrarComponent
      with WebSocketMessageSenderComponent =>

  val socketIoPacketSender: SocketIoPacketSender = new SocketIoPacketSenderImpl

  class SocketIoPacketSenderImpl extends SocketIoPacketSender {
    def send(connectionId: String, packet: SocketIoPacket) {
      connectionRegistrar.find(connectionId) map {
        clientConnection =>
          webSocketMessageSender.send(clientConnection.connectionId, packet.serialize())
      }
    }
  }

}
