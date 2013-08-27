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

import com.originate.play.common.CommonComponentRegistryImpl
import com.originate.play.websocket.plugins.{PluginsComponentRegistryImpl, PluginsComponentRegistry}
import com.originate.play.websocket.socketio.{SocketIoComponentRegistryImpl, SocketIoComponentRegistry}

trait WebSocketModuleComponentRegistry
    extends WebSocketModuleConfigComponent
    with WebSocketModuleActorsComponent
    with WebSocketMessageSenderComponent
    with WebSocketsControllerComponent
    with PluginsComponentRegistry
    with SocketIoComponentRegistry

trait WebSocketModuleComponentRegistryImpl
    extends CommonComponentRegistryImpl
    with WebSocketModuleConfigComponentImpl
    with WebSocketModuleActorsComponentImpl
    with WebSocketMessageSenderComponentImpl
    with WebSocketsControllerComponentImpl
    with PluginsComponentRegistryImpl
    with SocketIoComponentRegistryImpl
