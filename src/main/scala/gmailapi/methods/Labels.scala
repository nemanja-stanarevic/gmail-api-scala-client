package gmailapi.methods

import akka.actor.Actor
import gmailapi.oauth2.OAuth2Identity
import gmailapi.resources.{ Label, LabelSerializer }
import org.json4s.jackson.Serialization.{ read, write }
import spray.http.{ HttpCredentials, HttpEntity, HttpMethods, ContentTypes }

object Labels {
  import LabelSerializer._
  
  case class Create(label: Label, userId: String = "me")
    (implicit val token: OAuth2Identity) extends GmailRestRequest {

    val uri = s"$baseUri/users/$userId/labels"
    val method = HttpMethods.POST
    val credentials : Option[HttpCredentials] = token
    val entity : HttpEntity = HttpEntity(ContentTypes.`application/json`, write(label))
    val unmarshaller = Some(read[Label](_:String))
  } 
  
  case class Delete(id: String, userId: String = "me")
    (implicit val token: OAuth2Identity) extends GmailRestRequest {
  
    val uri = s"$baseUri/users/$userId/labels/$id"
    val method = HttpMethods.DELETE
    val credentials : Option[HttpCredentials] = token
    val entity = HttpEntity.Empty
    val unmarshaller = None
  }
  
  case class List(userId: String = "me")  
    (implicit val token: OAuth2Identity) extends GmailRestRequest {

    val uri = s"$baseUri/users/$userId/labels"
    val method = HttpMethods.GET
    val credentials : Option[HttpCredentials] = token
    val entity = HttpEntity.Empty
    val unmarshaller = Some(read[Seq[Label]](_:String))
  }
  
  case class Update(id: String, label: Label, userId: String = "me") 
    (implicit val token: OAuth2Identity) extends GmailRestRequest {
    
    val uri = s"$baseUri/users/$userId/labels/$id"
    val method = HttpMethods.PUT
    val credentials : Option[HttpCredentials] = token
    val entity = HttpEntity(ContentTypes.`application/json`, write(label))
    val unmarshaller = Some(read[Label](_:String))
  }
  
  case class Get(id: String, userId: String = "me") 
    (implicit val token: OAuth2Identity) extends GmailRestRequest {

    val uri = s"$baseUri/users/$userId/labels/$id"
    val method = HttpMethods.GET
    val credentials : Option[HttpCredentials] = token
    val entity = HttpEntity.Empty
    val unmarshaller = Some(read[Label](_:String))
  }
  
  case class Patch(id: String, label: Label, userId: String = "me") 
    (implicit val token: OAuth2Identity) extends GmailRestRequest {

    val uri = s"$baseUri/users/$userId/labels/$id" 
    val method = HttpMethods.PATCH
    val credentials : Option[HttpCredentials] = token
    val entity = HttpEntity(ContentTypes.`application/json`, write(label))
    val unmarshaller = Some(read[Label](_:String))
  }
}
  