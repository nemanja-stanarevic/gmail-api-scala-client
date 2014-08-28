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
package gmailapi.restclient

import spray.http.{ HttpMethod, HttpCredentials, HttpEntity }

object RestResponses {
  sealed trait RestResponse {
    val isFailure: Boolean
    val retryPolicy: Option[RetryPolicy]
  }

  case class Resource[+A](get: A) extends RestResponse {
    val isFailure = false
    val retryPolicy = RetryPolicy.None
  }

  case object NotFound extends RestResponse {
    val isFailure = false
    val retryPolicy = RetryPolicy.None
  }

  case object Done extends RestResponse {
    val isFailure = false
    val retryPolicy = RetryPolicy.None
  }

  case object ExpiredAuthToken extends RestResponse {
    val isFailure = true
    val retryPolicy = RetryPolicy.OnceImmediate
  }

  case object RateLimitExceeded extends RestResponse {
    val isFailure = false
    val retryPolicy: Option[RetryPolicy] = RetryPolicy.ExponentialBackoff
  }

  case class InvalidRequest(message: String) extends RestResponse {
    val isFailure = true
    val retryPolicy = RetryPolicy.None
  }

  case class Failure(statusCode: Int, message: String) extends RestResponse {
    val isFailure = true
    val retryPolicy: Option[RetryPolicy] = RetryPolicy.ExponentialBackoff
  }

  case class Exception(throwable: Throwable) extends RestResponse {
    val isFailure = true
    val retryPolicy = RetryPolicy.None
  }
}
