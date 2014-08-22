package gmailapi

import akka.actor.Actor
import akka.event.Logging
import org.json4s.jackson.Serialization.{read, write}
import spray.http.{ ContentTypes, StatusCodes }
import spray.httpx.unmarshalling._
import spray.client.pipelining._
import gmailapi._
import spray.http.OAuth2BearerToken
import gmailapi.restclient._
import gmailapi.resources.GmailResource

class GmailApiActor extends Actor with RestActor {
  type RestResource = GmailResource
  val log = Logging(context.system, this)
  val errorHandler = List(
    // depending on the uri, google will return 400 or 401 when token is either expired
    // or invalid, so we will pass along ExpiredAuthToken message to allow client to 
    // to attempt a refresh
    RestActor.ErrorHandler(StatusCodes.BadRequest, RestResponses.ExpiredAuthToken) {
    	_.contains("invalid_token") },
    RestActor.ErrorHandler(StatusCodes.Unauthorized, RestResponses.ExpiredAuthToken) {
    	_.contains("Invalid Credentials") }
    )
}
