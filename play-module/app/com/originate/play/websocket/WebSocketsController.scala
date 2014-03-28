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

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.originate.play.websocket.plugins.{ClientInformationProviderComponent, WebSocketHooksComponent, ConnectionRegistrarComponent}
import java.util.UUID
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee.{Iteratee, Concurrent}
import play.api.mvc._
import scala.concurrent.duration._

trait WebSocketsController
    extends Controller {
  def init: WebSocket[String]
}

trait WebSocketsControllerComponent {
  val webSocketsController: WebSocketsController
}

trait WebSocketsControllerComponentImpl
    extends WebSocketsControllerComponent {
  this: WebSocketModuleConfigComponent
      with ConnectionRegistrarComponent
      with WebSocketModuleActorsComponent
      with WebSocketHooksComponent
      with ClientInformationProviderComponent =>

  lazy val webSocketsController: WebSocketsController = new WebSocketsControllerImpl

  class WebSocketsControllerImpl
      extends WebSocketsController {
    def init = WebSocket.using[String] {
      implicit request =>
        clientInformationProvider.getClientInfo map {
          clientInfo =>
            val connectionId = UUID.randomUUID().toString
            val (outEnumerator, channel) = Concurrent.broadcast[String]
            val actorAddress = s"${webSocketModuleActors.actorSystemAddress}/user/$connectionId"

            val connection = ClientConnection(clientInfo, connectionId, actorAddress)
            val connectionActorRef = webSocketModuleActors.newActor(connection, channel)

            Logger.debug(s"WebSocket init $connection called from ${request.headers.get("X-Forwarded-For")}")

            val in = Iteratee.foreach[String] {
              msg =>
                Logger.debug(s"Message received $connection: $msg")
                val receive = webSocketHooks.messageReceivedHook
                receive(connection, msg)
            }.mapDone {
              _ =>
                connectionRegistrar.deregister(connection)
                shutdown(connection, connectionActorRef)
                Logger.debug(s"Client disconnected $connection")
            }

            connectionRegistrar.register(connection)

            val finalOutEnumerator = webSocketHooks.connectionEstablishedHook(connection, outEnumerator)
            (in, finalOutEnumerator)
        } getOrElse {
          throw new Exception("Cannot create a WebSocket connection without client Id")
        }
    }

    def shutdown(connection: ClientConnection, connectionActorRef: ActorRef) {
      val timeoutDuration = webSocketConfig.getDuration("connection.stop.timeout") getOrElse {
        Logger.warn("Cannot find 'connection.stop.timeout' parameter in websocket config, using 5 sec")
        5.seconds
      }

      implicit val timeout = Timeout(timeoutDuration)

      connectionActorRef ? Stop map {
        case Ack => Logger.debug(s"Actor stopping acknowledged $connection")
      }
    }
  }

}
