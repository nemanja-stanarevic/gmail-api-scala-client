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
package gmailapi.oauth2

import gmailapi.resources.GmailResource
import org.json4s.{ DefaultFormats, FieldSerializer }
import spray.httpx.Json4sJacksonSupport

case class OAuth2Identity(
  accessToken: String,
  refreshToken: String,
  expiration: Long,
  userId: Option[String] = None,
  email: Option[String] = None,
  scope: Seq[String] = Nil,
  name: Option[String] = None,
  givenName: Option[String] = None,
  familyName: Option[String] = None,
  picture: Option[String] = None,
  gender: Option[String] = None,
  locale: Option[String] = None) extends GmailResource
