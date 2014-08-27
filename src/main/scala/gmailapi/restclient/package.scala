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

import spray.http.{ HttpMethod, HttpCredentials, HttpEntity }

package object restclient {

  object RestResponses {
    sealed trait RestResponse
    case class Resource[+A](get: A) extends RestResponse
    case object Done extends RestResponse
    case object ExpiredAuthToken extends RestResponse
    case object NotFound extends RestResponse
    case class InvalidRequest(message: String) extends RestResponse
    case class Failure(statusCode: Int, message: String) extends RestResponse
    case class Exception(throwable: Throwable) extends RestResponse
    case object RateLimitExceeded extends RestResponse
  }

  trait RestRequest {
    val uri: String
    val method: HttpMethod
    val credentials: Option[HttpCredentials]
    val entity: HttpEntity
    val unmarshaller: Option[String => Any]
  }
}
