package gmailapi

import spray.http.{ HttpMethod, HttpCredentials, HttpEntity }

package object restclient {
    
  object RestResponses {
    sealed trait RestResponse
  	case class  Resource[+A](get: A) extends RestResponse
    case object Done extends RestResponse
    case object ExpiredAuthToken extends RestResponse
    case object NotFound extends RestResponse
    case class  InvalidRequest(message:String) extends RestResponse
    case class  Failure(statusCode: Int, message: String) extends RestResponse
    case class  Exception(throwable: Throwable) extends RestResponse
    case object RateLimitExceeded extends RestResponse
  }
  
  trait RestRequest {
    val uri : String
    val method : HttpMethod
    val credentials : Option[HttpCredentials]
    val entity: HttpEntity
    val unmarshaller: Option[String => Any]
  }
}