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

import play.api.libs.json.{JsNull, Json, JsValue}

object Acknowledge extends Enumeration {
  val Yes, No = Value

  def apply(ackValue: String): Value = if (ackValue == "+") Acknowledge.Yes else Acknowledge.No
}

sealed trait SocketIoPacket {
  val packetType: Int
  val messageId: String = ""
  val ack: Acknowledge.Value = Acknowledge.No
  val endpoint: String = ""
  val data: String = ""

  def isMessageDefined: Boolean = !messageId.isEmpty

  def isAckRequested: Boolean = ack == Acknowledge.Yes

  def isEndpointDefined: Boolean = !endpoint.isEmpty

  def isDataDefined: Boolean = !data.isEmpty

  def serialize(): String = {
    val builder = new StringBuilder()
        .append(packetType)
        .append(':')
        .append(messageId)
    if (isMessageDefined && isAckRequested) builder.append('+')
    builder.append(':')
        .append(endpoint)
    if (isDataDefined) builder.append(':').append(data)
    builder.toString()
  }
}

object SocketIoPacket {
  val Regexp = """(\d)(?::([\d]+)?(\+)?:([^:]+)?:?([\s\S]*)?)?""".r

  def apply(serializedSocketIoPacket: String): SocketIoPacket = {
    val Regexp(packetType, messageId, ackValue, endpoint, data) = serializedSocketIoPacket

    def toString(value: String) = Option(value) getOrElse ""
    def toJson(data: String): JsValue = if (data == null || data.trim.isEmpty) JsNull else Json.parse(data)

    packetType match {
      case "0" => Disconnect(toString(endpoint))
      case "1" => Connect(toString(endpoint), toString(data))
      case "2" => Heartbeat
      case "3" => Message(toString(messageId), Acknowledge(ackValue), toString(endpoint), toString(data))
      case "4" => JsonMessage(toString(messageId), Acknowledge(ackValue), toString(endpoint), toJson(data))
      case "5" => Event(toString(messageId), Acknowledge(ackValue), toString(endpoint), toJson(data))
      case "6" => Ack(toString(messageId), toString(data))
      case "7" =>
        val (reason, advice) = toString(data).span(_ != '+')
        Error(toString(endpoint), reason, if (advice.size > 1) advice.substring(1) else "")
      case "8" => Noop
      case _ => throw new IllegalArgumentException(s"Unexpected packet data received: $serializedSocketIoPacket")
    }
  }
}

case class Disconnect(
    override val endpoint: String = "")
    extends SocketIoPacket {
  val packetType = 0
}

case class Connect(
    override val endpoint: String = "",
    override val data: String = "")
    extends SocketIoPacket {
  val packetType = 1
}

case object Heartbeat
    extends SocketIoPacket {
  val packetType = 2
}

case class Message(
    override val messageId: String = "",
    override val ack: Acknowledge.Value = Acknowledge.No,
    override val endpoint: String = "",
    override val data: String = "")
    extends SocketIoPacket {
  val packetType = 3
}

case class JsonMessage(
    override val messageId: String = "",
    override val ack: Acknowledge.Value = Acknowledge.No,
    override val endpoint: String = "",
    json: JsValue = JsNull)
    extends SocketIoPacket {
  val packetType = 4

  override val data: String = if (json == JsNull) "" else Json.stringify(json)
}

case class Event(
    override val messageId: String = "",
    override val ack: Acknowledge.Value = Acknowledge.No,
    override val endpoint: String = "",
    json: JsValue = JsNull)
    extends SocketIoPacket {
  val packetType = 5

  override val data: String = if (json == JsNull) "" else Json.stringify(json)
}

case class Ack(
    override val messageId: String = "",
    override val data: String = "")
    extends SocketIoPacket {
  val packetType = 6
}

case class Error(
    override val endpoint: String = "",
    reason: String = "",
    advice: String = "")
    extends SocketIoPacket {
  val packetType = 7

  override val data: String = if (!reason.isEmpty || !advice.isEmpty) reason + '+' + advice else ""
}

case object Noop
    extends SocketIoPacket {
  val packetType = 8
}
