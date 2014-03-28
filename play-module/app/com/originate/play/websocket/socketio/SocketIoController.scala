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

import com.originate.play.websocket.WebSockets
import com.originate.play.websocket.plugins.ClientInformationProviderComponent
import play.api.Logger
import play.api.mvc.{Controller, WebSocket, AnyContent, Action}
import scala.concurrent.duration._

trait SocketIoController extends Controller {
  def init(socketUrl: String): WebSocket[String]

  def initSession(socketUrl: String): Action[AnyContent]
}

trait SocketIoControllerComponent {
  val socketIoController: SocketIoController
}

trait SocketIoControllerComponentImpl
    extends SocketIoControllerComponent {
  this: SocketIoConfigComponent
      with ClientInformationProviderComponent =>

  lazy val socketIoController: SocketIoController = new SocketIoControllerImpl

  class SocketIoControllerImpl extends SocketIoController {

    def init(socketUrl: String): WebSocket[String] = WebSockets.init

    def initSession(socketUrl: String) = Action {
      implicit request =>
        clientInformationProvider.getClientInfo map {
          clientInfo =>
            val sessionId = clientInfo.clientId
            Logger.debug(s"SocketIo session is being initiated: $sessionId [$clientInfo]")
            val heartbeatInterval = socketIoConfig.getDuration("heartbeat.interval") getOrElse {
              Logger.warn("Cannot find 'heartbeat.interval' parameter in socketio config, using 30 sec")
              30.seconds
            }
            val connectionTimeout = socketIoConfig.getDuration("connection.timeout") getOrElse {
              Logger.warn("Cannot find 'connection.timeout' parameter in socketio config, using 10 min")
              10.minutes
            }
            Ok(s"$sessionId:${heartbeatInterval.toSeconds}:${connectionTimeout.toSeconds}:websocket")
        } getOrElse {
          Unauthorized
        }
    }
  }

}
