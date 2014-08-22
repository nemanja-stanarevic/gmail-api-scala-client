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
    (val filter: String => Boolean = _ => true)
}

trait RestActor {
  implicit val context: ActorContext
  import context.dispatcher
  import RestActor._
  
  type RestResource
  
  protected val errorHandler : Seq[ErrorHandler]
  protected val log : LoggingAdapter
  
  private def httpPipeline(
    implicit refFactory: ActorRefFactory, 
    executionContext: ExecutionContext) : 
    HttpRequest => Future[HttpResponse] = 
    (  encode(Gzip)
    ~> sendReceive
    ~> decode(Deflate) )

  private def httpPipeline(cred: HttpCredentials)(
    implicit refFactory: ActorRefFactory, 
    executionContext: ExecutionContext) : 
    HttpRequest => Future[HttpResponse] = 
    (  addCredentials(cred)
    ~> encode(Gzip)
    ~> sendReceive
    ~> decode(Deflate) )
  
  def receive : Actor.Receive = {
    case message : RestRequest =>
      val httpRequest = HttpRequest(
        method = message.method,
        uri = message.uri,
        entity = message.entity)

      val pipeline = message.credentials match {
	    case Some(cred) => httpPipeline(cred)
	    case None => httpPipeline
	  }

      val client = context.sender
      val unmarshaller = message.unmarshaller
      val startTimestamp = System.currentTimeMillis
	  pipeline(httpRequest) onComplete {
        case Success(response @ HttpResponse(StatusCodes.OK, _, _, _)) => 
          val doneTimestamp = System.currentTimeMillis
          log.info("Lap time [{}] sec for [{}].", 
              (doneTimestamp-startTimestamp)/1000.0, 
              message.getClass())
          unmarshaller match {
            case Some(unmarshall) =>
              val resource = unmarshall( response.entity.data.asString )
              client ! RestResponses.Resource(resource)
            case None =>
              client ! RestResponses.Done
          }
        case Success(response @ HttpResponse(statusCode, entity, _, _)) => 
          log.info("Rest service returned an error.  Request: [{}] Response: [{}]", 
              httpRequest,
              response)
              
          var _fired = false
          errorHandler foreach { handler =>
            if ( statusCode == handler.statusCode & 
                 (handler.filter apply entity.data.asString) & 
                 !_fired) {
              client ! handler.response
              _fired = true
            }
	      }
          if (!_fired)
            client ! RestResponses.Failure(statusCode.intValue, entity.data.asString) 
        case Failure(e) =>
          log.error(e, "Rest client failed due to [{}].", 
              e.getMessage)
              
          client ! RestResponses.Exception(e)
      }	  
  }
  
}