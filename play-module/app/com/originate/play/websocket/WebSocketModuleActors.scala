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

import com.originate.common.{Shutdownable, BaseComponent}
import akka.actor._
import akka.pattern.ask
import akka.remote.RemoteActorRefProvider
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import scala.concurrent._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee.Concurrent
import akka.util.Timeout
import play.api.libs.concurrent.Akka
import play.api.Play.current

trait WebSocketModuleActorsComponent extends BaseComponent {
  val webSocketModuleActors: WebSocketModuleActors

  trait WebSocketModuleActors {
    def actorSystemAddress: Address

    def findActor(actorAddress: String): ActorRef

    def newActor(clientId: String, connectionId: String, channel: Concurrent.Channel[String]): Shutdownable
  }

}

trait WebSocketModuleActorsComponentImpl extends WebSocketModuleActorsComponent {
  this: WebSocketModuleConfigComponent =>

  val webSocketModuleActors: WebSocketModuleActors = new WebSocketModuleActorsImpl

  def actorSystem = Akka.system.asInstanceOf[ExtendedActorSystem]

  case object Stop

  case object Ack

  class WebSocketActor(
      val clientId: String,
      val connectionId: String,
      val channel: Concurrent.Channel[String]) extends Actor {
    def receive = {
      case Stop =>
        Logger.info(s"Stop received")
        actorSystem.stop(self)
        // TODO(dtarima): do we need the ack? if yes then should it be a class with specific Stop message (unique)
        sender ! Ack
      case x =>
        Logger.info(s"Message received: $x [pushing to $connectionId]")
        channel.push(x.toString)
    }
  }

  class WebSocketModuleActorsImpl extends WebSocketModuleActors {

    def actorSystemAddress = actorSystem.provider match {
      case rarp: RemoteActorRefProvider => rarp.transport.address
      case _ => actorSystem.provider.rootPath.address
    }

    def findActor(actorAddress: String): ActorRef = actorSystem.actorFor(actorAddress)

    def newActor(clientId: String, connectionId: String, channel: Concurrent.Channel[String]) = {
      val actorRef = actorSystem.actorOf(
        Props({
          new WebSocketActor(clientId, connectionId, channel)
        }), name = connectionId)
      new Shutdownable() {
        def shutdown() {
          val timeoutMs = webSocketConfig.getMilliseconds("connection.stop.timeout") getOrElse {
            Logger.warn("Cannot find 'connection.stop.timeout' parameter in websocket config, using 5 sec")
            5000L
          }
          implicit val timeout = Timeout(timeoutMs)

          actorRef ? Stop map {
            case Ack => Logger.info(s"Actor stopping acknowledged [$clientId] $connectionId")
          } onFailure {
            // TODO(dtarima): it would leak, but is it possible?
            case _ => Logger.error(s"Actor failed to stop [$clientId] $connectionId")
          }
        }
      }
    }
  }

  override def onInit() {
    try {
      val timeoutMs = webSocketConfig.getMilliseconds("actor.system.init.timeout") getOrElse {
        Logger.warn("Cannot find 'actor.system.init.timeout' parameter in websocket config, using 5 sec")
        5000L
      }
      val timeout = Duration(timeoutMs, TimeUnit.MILLISECONDS)
      Await.ready(Future(actorSystem), timeout)
    } catch {
      case e: TimeoutException =>
        Logger.error("WebSocketModule ActorSystem failed to initialize during allotted time interval", e)
        throw e
      case e: Throwable =>
        Logger.error("Unexpected error during WebSocketModule ActorSystem initialization", e)
        throw e
    }

    super.onInit()
  }
}
