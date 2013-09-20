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
package com.originate.common.config

import com.originate.common.BaseComponent
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{FiniteDuration, Duration}

trait Config {
  def getBoolean(path: String): Option[Boolean]

  def getBytes(path: String): Option[Long]

  def getConfig(path: String): Option[Config]

  def getInt(path: String): Option[Int]

  def getIntList(path: String): Option[List[Int]]

  def getMilliseconds(path: String): Option[Long]

  def getString(path: String, validValues: Option[Set[String]] = None): Option[String]

  def getStringList(path: String): Option[List[String]]

  def withFallback(fallbackConfig: Config): Config

  def hasPath(path: String): Boolean

  def getDuration(path: String): Option[FiniteDuration] =
    getMilliseconds(path) map (Duration.create(_, TimeUnit.MILLISECONDS))
}

trait ConfigComponent extends BaseComponent {
  val config: Config
}

trait SubConfigComponent extends ConfigComponent {
  this: ConfigComponent =>

  protected def getSubConfig(configPath: String): Config = {
    val defaultConfigPath = configPath + "_defaults"

    val defaultConfig = config.getConfig(defaultConfigPath) getOrElse {
      throw new IllegalStateException( s"""Please include "$configPath.conf" in your application.conf""")
    }

    config.getConfig(configPath) map (_.withFallback(defaultConfig)) getOrElse defaultConfig
  }
}
