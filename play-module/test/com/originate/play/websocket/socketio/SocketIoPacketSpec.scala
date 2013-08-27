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

import org.specs2.mutable.Specification
import play.api.libs.json.Json

class SocketIoPacketSpec extends Specification {
  "SocketIoPacket" should {
    "serialize correctly '0::/test'" in {
      val data = "0::/test"
      val packet = Disconnect("/test")
      SocketIoPacket(data) === packet
      packet.serialize() === data
    }

    "serialize correctly '0'" in {
      val data = "0"
      val packet = Disconnect()
      SocketIoPacket(data) === packet
      packet.serialize() === data + "::"
    }

    "serialize correctly '1::'" in {
      val data = "1::"
      val packet = Connect()
      SocketIoPacket(data) === packet
      packet.serialize() === data
    }

    "serialize correctly '1::/test?my=param'" in {
      val data = "1::/test?my=param"
      val packet = Connect("/test?my=param")
      SocketIoPacket(data) === packet
      packet.serialize() === data
    }

    "serialize correctly '2'" in {
      val data = "2"
      val packet = Heartbeat
      SocketIoPacket(data) === packet
      packet.serialize() === data + "::"
    }

    "serialize correctly '3:1::blabla'" in {
      val data = "3:1::blabla"
      val packet = Message(messageId = "1", data = "blabla")
      SocketIoPacket(data) === packet
      packet.serialize() === data
    }

    """serialize correctly '4:1::{"a":"b"}'""" in {
      val data = """4:1::{"a":"b"}"""
      val packet = JsonMessage(messageId = "1", json = Json.parse( """{"a":"b"}"""))
      SocketIoPacket(data) === packet
      packet.serialize() === data
    }

    "serialize correctly '5::'" in {
      val data = "5::"
      val packet = Event()
      SocketIoPacket(data) === packet
      packet.serialize() === data
    }

    """serialize correctly '5:::{"name":"hi2!"}'""" in {
      val data = """5:::{"name":"hi2!"}"""
      val packet = Event(json = Json.parse( """{"name":"hi2!"}"""))
      SocketIoPacket(data) === packet
      packet.serialize() === data
    }

    """serialize correctly '5:2+::{"name":"test"}'""" in {
      val data = """5:2+::{"name":"test"}"""
      val packet = Event(messageId = "2", ack = Acknowledge.Yes, json = Json.parse("""{"name":"test"}"""))
      SocketIoPacket(data) === packet
      packet.serialize() === data
    }

    "serialize correctly '6:::4'" in {
      val data = "6:::4"
      val packet = Ack(data = "4")
      SocketIoPacket(data) === packet
      packet.serialize() === data
    }

    """serialize correctly '6:::4+["A","B"]'""" in {
      val data = """6:::4+["A","B"]"""
      val packet = Ack(data = """4+["A","B"]""")
      SocketIoPacket(data) === packet
      packet.serialize() === data
    }

    "serialize correctly '7::'" in {
      val data = "7::"
      val packet = Error()
      SocketIoPacket(data) === packet
      packet.serialize() === data
    }

    "serialize correctly '7::/test:reason+advice'" in {
      val data = "7::/test:reason+advice"
      val packet = Error("/test", "reason", "advice")
      SocketIoPacket(data) === packet
      packet.serialize() === data
    }

    "serialize correctly '8'" in {
      val data = "8"
      val packet = Noop
      SocketIoPacket(data) === packet
      packet.serialize() === data + "::"
    }
  }
}
