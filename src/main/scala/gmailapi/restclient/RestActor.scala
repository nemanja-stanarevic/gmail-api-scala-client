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

import akka.actor._
import akka.event.{ LoggingReceive, Logging, LoggingAdapter }
import scala.collection.immutable.Seq
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Try, Success, Failure }
import scala.util.matching.Regex
import spray.client.pipelining._
import spray.http.{ HttpCredentials, HttpRequest, HttpResponse, StatusCode, StatusCodes }
import spray.httpx.encoding.{ Gzip, Deflate }

object RestActor {
  case class ErrorHandler(
    statusCode: StatusCode,
    response: RestResponses.RestResponse)
    (val filter: (String => Boolean) = (_: String) => true)
}

trait RestActor {
  implicit val context: ActorContext
  import context.dispatcher
  import RestActor._

  type RestResource

  protected val errorHandler: Seq[ErrorHandler]
  protected val log: LoggingAdapter

  private def httpPipeline(
    implicit refFactory: ActorRefFactory,
    executionContext: ExecutionContext): HttpRequest => Future[HttpResponse] =
    (encode(Gzip)
      ~> sendReceive
      ~> decode(Deflate))

  private def httpPipeline(cred: HttpCredentials)(
    implicit refFactory: ActorRefFactory,
    executionContext: ExecutionContext): HttpRequest => Future[HttpResponse] =
    (addCredentials(cred)
      ~> encode(Gzip)
      ~> sendReceive
      ~> decode(Deflate))

  def receive: Actor.Receive = {
    case message: RestRequest =>
      val httpRequest = HttpRequest(
        method = message.method,
        uri = message.uri,
        entity = message.entity)

      val pipeline = message.credentials match {
        case Some(cred) => httpPipeline(cred)
        case None       => httpPipeline
      }

      val client = context.sender
      val unmarshaller = message.unmarshaller
      val startTimestamp = System.currentTimeMillis

      pipeline(httpRequest) onComplete {
        case Success(response @ HttpResponse(StatusCodes.OK, _, _, _)) =>
          val doneTimestamp = System.currentTimeMillis
          log.info("Lap time [{}] sec for [{}].",
            (doneTimestamp - startTimestamp) / 1000.0,
            message.getClass())
          unmarshaller match {
            case Some(unmarshall) =>
              val resource = unmarshall(response.entity.data.asString)
              client ! RestResponses.Resource(resource)
            case None =>
              client ! RestResponses.Done
          }
        // Rest service may return 204 NoContent when there is no response
        case Success(response @ HttpResponse(StatusCodes.NoContent, _, _, _)) =>
          val doneTimestamp = System.currentTimeMillis
          log.info("Lap time [{}] sec for [{}].",
            (doneTimestamp - startTimestamp) / 1000.0,
            message.getClass())
          client ! RestResponses.Done
        case Success(response @ HttpResponse(statusCode, entity, _, _)) =>
          log.info("Rest service returned an error.  Request: [{}] Response: [{}]",
            httpRequest,
            response)

          var _fired = false
          errorHandler foreach { handler =>
            if (statusCode == handler.statusCode &
              (handler.filter apply entity.data.asString) &
              !_fired) {
              client ! handler.response
              _fired = true
            }
          }
          if (!_fired) {
            client ! RestResponses.Failure(
              statusCode.intValue,
              entity.data.asString)
          }
        case Failure(e) =>
          log.error(e, "Rest client failed due to [{}].",
            e.getMessage)

          client ! RestResponses.Exception(e)
      }
  }
}
