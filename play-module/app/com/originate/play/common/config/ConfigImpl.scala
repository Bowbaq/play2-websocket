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
package com.originate.play.common.config

import scala.language.postfixOps
import play.api.{Configuration, Play}
import collection.JavaConversions._
import com.typesafe.config.ConfigFactory
import com.originate.common.config.{ConfigComponent, Config}

trait ConfigComponentImpl extends ConfigComponent {
  lazy val config: Config = new ConfigImpl

  class ConfigImpl(private val playConfiguration: Option[Configuration] = ConfigComponentImpl.getConfig)
      extends Config {
    def getBoolean(path: String) = playConfiguration flatMap (_.getBoolean(path))

    def getBytes(path: String) = playConfiguration flatMap (_.getBytes(path))

    def getConfig(path: String) = playConfiguration map (_.getConfig(path)) map (new ConfigImpl(_))

    def getInt(path: String) = playConfiguration flatMap (_.getInt(path))

    def getIntList(path: String) = playConfiguration flatMap (_.getIntList(path)) map (_ map (_.toInt) toList)

    def getMilliseconds(path: String) = playConfiguration flatMap (_.getMilliseconds(path))

    def getString(path: String, validValues: Option[Set[String]]) =
      playConfiguration flatMap (_.getString(path, validValues))

    def getStringList(path: String) = playConfiguration flatMap (_.getStringList(path)) map (_ map ("" + _) toList)

    def withFallback(fallbackConfig: Config): Config = {
      val underlyingConfigOpt = playConfiguration map (_.underlying)

      val underlyingFallbackConfigOpt = fallbackConfig match {
        case x: ConfigImpl => x.playConfiguration map (_.underlying)
        case _ => None
      }

      if (underlyingConfigOpt.isEmpty) fallbackConfig
      else if (underlyingFallbackConfigOpt.isEmpty) this
      else new ConfigImpl(Some(Configuration(underlyingConfigOpt.get.withFallback(underlyingFallbackConfigOpt.get))))
    }

    def hasPath(path: String) = playConfiguration exists (_.underlying.hasPath(path))
  }

}

object ConfigComponentImpl {
  def getConfig: Option[Configuration] = {
    Play.maybeApplication map (_.configuration) orElse Some(Configuration(ConfigFactory.load()))
  }
}