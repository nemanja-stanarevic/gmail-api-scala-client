/*
 * Copyright © 2014 Nemanja Stanarevic <nemanja@alum.mit.edu>
 *
 * Made with ❤ in NYC at Hacker School <http://hackerschool.com>
 *
 * Licensed under the GNU Affero General Public License, Version 3
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 *
 * <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gmailapi

import com.typesafe.config.Config
import scala.language.postfixOps
import spray.http.Uri

package object oauth2 {

  def authorizationUri(loginHint: Option[String] = None)
    (implicit config: Config): String =
    Uri("https://accounts.google.com/o/oauth2/auth") withQuery (
      "response_type" -> "code",
      "client_id" -> config.getString("oauth2.clientId"),
      "redirect_uri" -> config.getString("oauth2.redirectUri"),
      "scope" -> config.getString("oauth2.scope"),
      "access_type" -> "offline",
      "approval_prompt" -> "force",
      "login_hint" ->
      (loginHint match {
        case Some(x) => x
        case None    => ""
      })) toString
}
