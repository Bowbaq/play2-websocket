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
package plugins

import com.originate.play.websocket.plugins.ClientInformationProviderPlugin
import play.api.mvc.RequestHeader
import com.originate.play.websocket.ClientInfo
import play.api.Logger

class ClientInformationProviderPluginImpl(val app: play.Application)
    extends ClientInformationProviderPlugin {
  def getClientInfo(implicit request: RequestHeader): Option[ClientInfo] = {
    Logger.info(s"${getClass.getSimpleName}: ClientInfo requested")

    request.session.get("s") map (sessionId => {
      ClientInfo("a_user_id", sessionId)
    })
  }

}
