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

import play.api.{Logger, Plugin}

class WebSocketModuleInitPlugin(val app: play.Application) extends Plugin {

  override def onStart() {
    Logger.debug("Initializing WebSocketModule")
    try {
      ComponentRegistry.main.onInit()
    } catch {
      case e: Throwable =>
        Logger.error("Failed to start WebSocketModule successfully", e)
        throw e
    }
  }

  override def onStop() {
    Logger.debug("Stopping WebSocketModule")
    try {
      ComponentRegistry.main.onShutdown()
    } catch {
      case e: Throwable =>
        Logger.error("Failed to stop WebSocketModule successfully", e)
        throw e
    }
  }

  override def enabled = true
}
