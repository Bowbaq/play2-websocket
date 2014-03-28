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

import akka.actor._
import akka.remote.RemoteActorRefProvider
import com.originate.common.BaseComponent
import com.originate.play.websocket.plugins.WebSocketHooksComponent
import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee.Concurrent
import scala.concurrent._
import scala.concurrent.duration._

trait WebSocketModuleActorsComponent extends BaseComponent {
  val webSocketModuleActors: WebSocketModuleActors

  case object Stop

  case object Ack

  trait WebSocketModuleActors {
    def actorSystem: ExtendedActorSystem

    def actorSystemAddress: Address

    def findActor(actorAddress: String): ActorRef

    def newActor(clientConnection: ClientConnection, channel: Concurrent.Channel[String]): ActorRef
  }

}

trait WebSocketModuleActorsComponentImpl extends WebSocketModuleActorsComponent {
  this: WebSocketModuleConfigComponent
      with WebSocketHooksComponent =>

  val webSocketModuleActors: WebSocketModuleActors = new WebSocketModuleActorsImpl

  class WebSocketModuleActorsImpl extends WebSocketModuleActors {

    class WebSocketActor(
        val clientConnection: ClientConnection,
        val channel: Concurrent.Channel[String]) extends Actor {
      def receive = {
        case Stop =>
          Logger.debug(s"WebSocketActor: Stop received")
          // TODO(dtarima): do we need the ack? if yes then should it be a class with specific Stop message (unique)
          sender ! Ack
          channel.eofAndEnd()
          actorSystem.stop(self)
        case x =>
          Logger.debug(s"WebSocketActor: Message received: $x [pushing to $clientConnection]")
          val message = x.toString
          channel.push(message)
          val messageSent = webSocketHooks.messageSentHook
          messageSent(clientConnection, message)

      }
    }

    lazy val actorSystem = {
      val timeout = webSocketConfig.getDuration("actor.system.init.timeout") getOrElse {
        Logger.warn("Cannot find 'actor.system.init.timeout' parameter in websocket config, using 5 sec")
        5.seconds
      }
      try {
        Await.ready(Future(Akka.system.asInstanceOf[ExtendedActorSystem]), timeout)
      } catch {
        case e: TimeoutException =>
          Logger.error("WebSocketModule ActorSystem failed to initialize during allotted time interval", e)
          throw e
        case e: Throwable =>
          Logger.error("Unexpected error during WebSocketModule ActorSystem initialization", e)
          throw e
      }
      Akka.system.asInstanceOf[ExtendedActorSystem]
    }

    def actorSystemAddress = actorSystem.provider match {
      case rarp: RemoteActorRefProvider => rarp.transport.address
      case _ => actorSystem.provider.rootPath.address
    }

    def findActor(actorAddress: String): ActorRef = actorSystem.actorFor(actorAddress)

    def newActor(clientConnection: ClientConnection, channel: Concurrent.Channel[String]) = {
      actorSystem.actorOf(
        Props({
          new WebSocketActor(clientConnection, channel)
        }), name = clientConnection.connectionId)
    }
  }

}
