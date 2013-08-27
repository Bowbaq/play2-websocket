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

import play.api.mvc._
import play.api.libs.iteratee.{Iteratee, Concurrent}
import play.api.Logger
import java.util.UUID
import com.originate.play.websocket.plugins.{ClientInformationProviderComponent, WebSocketHooksComponent, ConnectionRegistrarComponent}
import com.originate.utils.SystemInfo

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
    val hostname = SystemInfo.hostname

    def init = WebSocket.using[String] {
      implicit request =>
        val clientInfo = clientInformationProvider.getClientInfo getOrElse (
            throw new Exception("Cannot create a WebSocket connection without client Id"))

        val clientId = clientInfo.clientId
        val connectionId = UUID.randomUUID().toString
        val (outEnumerator, channel) = Concurrent.broadcast[String]
        val connectionActor = webSocketModuleActors.newActor(clientId, connectionId, channel)
        val actorAddress = s"${webSocketModuleActors.actorSystemAddress}/user/$connectionId"
        val connection = ClientConnection(clientInfo, connectionId, actorAddress)
        Logger.info(s"WebSocket init $connection called from ${request.headers.get("X-Forwarded-For")}")

        val in = Iteratee.foreach[String] {
          msg =>
            Logger.info(s"Message received $connection: $msg")
            val receive = webSocketHooks.messageReceivedHook
            receive(connection, msg)
        }.mapDone {
          _ =>
            Logger.info(s"Client disconnected $connection")
            connectionActor.shutdown()
            connectionRegistrar.deregister(connection)
        }

        connectionRegistrar.register(connection)

        val finalOutEnumerator = webSocketHooks.connectionEstablishedHook(connection, outEnumerator)
        (in, finalOutEnumerator)
    }
  }

}
