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

import gmailapi.restclient.RestRequest
import gmailapi.oauth2.OAuth2Identity
import scala.language.implicitConversions
import spray.http.{ HttpCredentials, OAuth2BearerToken }

package object methods {

  trait GmailRestRequest extends RestRequest {
    val baseUri = "https://www.googleapis.com/gmail/v1"
    val quotaUnits : Int
  }

  implicit def oauth2IdToBearerToken(id: OAuth2Identity): Option[HttpCredentials] =
    Some(OAuth2BearerToken(id.accessToken))
}
