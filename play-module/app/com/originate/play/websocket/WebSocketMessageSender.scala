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
package com.originate.play.websocket

import com.originate.play.websocket.plugins.ConnectionRegistrarComponent
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration.FiniteDuration

trait WebSocketMessageSender {
  def disconnect(connectionId: String)

  // TODO(dtarima): return status of sending the message? (we don't want to expose the actor too much)
  // TODO(dtarima): add sendAsk method?
  def send(connectionId: String, message: String): Unit

  def scheduleOnce(delay: FiniteDuration, connectionId: String, message: String): Unit
}

trait WebSocketMessageSenderComponent {
  val webSocketMessageSender: WebSocketMessageSender
}

trait WebSocketMessageSenderComponentImpl extends WebSocketMessageSenderComponent {
  this: ConnectionRegistrarComponent
      with WebSocketModuleActorsComponent =>

  val webSocketMessageSender: WebSocketMessageSender = new WebSocketMessageSenderImpl

  class WebSocketMessageSenderImpl extends WebSocketMessageSender {
    def disconnect(connectionId: String) {
      connectionRegistrar.find(connectionId) map {
        clientConnection =>
          webSocketModuleActors.findActor(clientConnection.connectionActorUrl) ! Stop
      }
    }

    def send(connectionId: String, message: String) {
      // TODO(dtarima): handle failures, like cannot find connection, cannot find actor, cannot send message (ask?)
      connectionRegistrar.find(connectionId) map {
        clientConnection =>
          webSocketModuleActors.findActor(clientConnection.connectionActorUrl) ! message
      }
    }

    def scheduleOnce(delay: FiniteDuration, connectionId: String, message: String) {
      connectionRegistrar.find(connectionId) map {
        clientConnection =>
          val actorRef = webSocketModuleActors.findActor(clientConnection.connectionActorUrl)
          Logger.debug(s"Schedule message to send in $delay [$connectionId]: $message")
          webSocketModuleActors.actorSystem.scheduler.scheduleOnce(delay, actorRef, message)
      }
    }
  }

}
