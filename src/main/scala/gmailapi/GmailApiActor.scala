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

import akka.actor.Actor
import akka.event.Logging
import org.json4s.jackson.Serialization.{ read, write }
import spray.http.{ ContentTypes, StatusCodes }
import spray.httpx.unmarshalling._
import spray.client.pipelining._
import gmailapi._
import spray.http.OAuth2BearerToken
import gmailapi.restclient._
import gmailapi.resources.GmailResource

class GmailApiActor extends Actor with RestActor {
  val log = Logging(context.system, this)
  val errorHandler = List(
    // Depending on the uri, google will return 400 or 401 when token is either
    // expired or invalid, so we will pass along ExpiredAuthToken message to
    // allow client to attempt a refresh
    RestActor.ErrorHandler(
      StatusCodes.BadRequest,
      RestResponses.ExpiredAuthToken) {
        _.contains("invalid_token")
      },
    RestActor.ErrorHandler(
      StatusCodes.Unauthorized,
      RestResponses.ExpiredAuthToken) {
        _.contains("Invalid Credentials")
      },
    // Gmail Api returns 404 Not Found when non-existent resource is
    // requested on Get and Patch
    RestActor.ErrorHandler(StatusCodes.NotFound, RestResponses.NotFound)(),
    // However, Gmail Api is a bit inconsistent, since Update and Delete return
    // 400 Invalid Request when we try to update/delete a non-existent resource,
    // we will return NotFound for uniform error handling
    RestActor.ErrorHandler(StatusCodes.BadRequest, RestResponses.NotFound) {
      _.contains("Invalid update request")
    },
    RestActor.ErrorHandler(StatusCodes.BadRequest, RestResponses.NotFound) {
      _.contains("Invalid delete request")
    },
    RestActor.ErrorHandler(StatusCodes.BadRequest, RestResponses.NotFound) {
      _.contains("Invalid id value")
    },
    // otherwise, pass the error along...
    RestActor.ErrorHandler(
      StatusCodes.BadRequest,
      RestResponses.InvalidRequest("Invalid label")) {
        _.contains("Invalid label")
      },
    RestActor.ErrorHandler(
      StatusCodes.TooManyRequests,
      RestResponses.RateLimitExceeded)() )
}
