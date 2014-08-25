package gmailapi.methods

import akka.actor.Actor
import gmailapi.oauth2.OAuth2Identity
import gmailapi.resources.{ History, HistoryList, GmailSerializer }
import org.json4s.jackson.Serialization.{ read, write }
import org.json4s.jackson.JsonMethods.parse
import scala.collection.immutable.Map
import spray.http.{ HttpCredentials, HttpEntity, HttpMethods, ContentTypes, Uri }

object Histories {
  import GmailSerializer._
  

  case class List(
    startHistoryId: Long,
    labelIds: Seq[String] = Nil,
    maxResults: Option[Int] = None,
    pageToken: Option[String] = None,
    userId: String = "me")  
    (implicit val token: OAuth2Identity) extends GmailRestRequest {

    val uri = {
      val queryBuilder = Uri.Query.newBuilder
      queryBuilder += ("startHistoryId" -> startHistoryId.toString)
      labelIds foreach {labelIds => queryBuilder += ("labelIds" -> labelIds) }
      maxResults foreach {maxResults => queryBuilder += ("maxResults" -> maxResults.toString) }
      pageToken foreach {pageToken => queryBuilder += ("pageToken" -> pageToken) }
      
      Uri(s"$baseUri/users/$userId/history") withQuery (queryBuilder.result()) toString
    }
    val method = HttpMethods.GET
    val credentials : Option[HttpCredentials] = token
    val entity = HttpEntity.Empty
    val unmarshaller = Some(read[HistoryList](_:String))
  }
}
  