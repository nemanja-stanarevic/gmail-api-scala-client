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
package gmailapi.methods

import akka.actor.Actor
import gmailapi.oauth2.OAuth2Identity
import gmailapi.resources.GmailSerializer
import gmailapi.resources.{ Message, MessageFormat, MessageAttachment }
import gmailapi.resources.{ MessageHeader, MessagePart, MessageList }
import org.json4s.jackson.Serialization.{ read, write }
import org.json4s.jackson.JsonMethods.parse
import scala.collection.immutable.Map
import scala.language.postfixOps
import spray.http.{HttpCredentials, HttpEntity, HttpMethods, ContentTypes, Uri}

object Messages {
  import GmailSerializer._

  case class Delete(id: String, userId: String = "me")
    (implicit val token: OAuth2Identity) extends GmailRestRequest {

    val uri = s"$baseUri/users/$userId/messages/$id"
    val method = HttpMethods.DELETE
    val credentials: Option[HttpCredentials] = token
    val entity = HttpEntity.Empty
    val unmarshaller = None
    val quotaUnits = 10
  }

  case class Get(
    id: String,
    format: MessageFormat.Value = MessageFormat.Full,
    userId: String = "me")
    (implicit val token: OAuth2Identity) extends GmailRestRequest {

    val uri = Uri(s"$baseUri/users/$userId/messages/$id") withQuery (
      Map("format" -> format.toString)) toString
    val method = HttpMethods.GET
    val credentials: Option[HttpCredentials] = token
    val entity = HttpEntity.Empty
    val unmarshaller = Some(read[Message](_: String))
    val quotaUnits = 5
  }

  case class Insert(message: Message, userId: String = "me")
    (implicit val token: OAuth2Identity) extends GmailRestRequest {
    assert(message.id == None)
    assert(message.snippet == None)
    assert(message.historyId == None)
    assert(message.payload == None)
    assert(message.sizeEstimate == None)
    assert(message.raw != None)

    val uri = s"$baseUri/users/$userId/messages"
    val method = HttpMethods.POST
    val credentials: Option[HttpCredentials] = token
    val entity: HttpEntity = HttpEntity(
      ContentTypes.`application/json`,
      write(message))
    val unmarshaller = Some(read[Message](_: String))
    val quotaUnits = 10
  }

  case class List(
    includeSpamTrash: Boolean = false,
    labelIds: Seq[String] = Nil,
    maxResults: Option[Int] = None,
    pageToken: Option[String] = None,
    query: Option[String] = None,
    userId: String = "me")
    (implicit val token: OAuth2Identity) extends GmailRestRequest {

    val uri = {
      val queryBuilder = Uri.Query.newBuilder
      queryBuilder += ("includeSpamTrash" -> includeSpamTrash.toString)
      labelIds foreach {
        labelIds => queryBuilder += ("labelIds" -> labelIds)
      }
      maxResults foreach {
        maxResults => queryBuilder += ("maxResults" -> maxResults.toString)
      }
      pageToken foreach {
        pageToken => queryBuilder += ("pageToken" -> pageToken)
      }
      query foreach {
        query => queryBuilder += ("query" -> query)
      }
      Uri(s"$baseUri/users/$userId/messages") withQuery (
        queryBuilder.result()) toString
    }
    val method = HttpMethods.GET
    val credentials: Option[HttpCredentials] = token
    val entity = HttpEntity.Empty
    val unmarshaller = Some(read[MessageList](_: String))
    val quotaUnits = 10
  }

  case class Modify(
    id: String,
    addLabelIds: Seq[String] = Nil,
    removeLabelIds: Seq[String] = Nil,
    userId: String = "me")
    (implicit val token: OAuth2Identity) extends GmailRestRequest {

    val uri = s"$baseUri/users/$userId/messages/$id/modify"
    val method = HttpMethods.POST
    val credentials: Option[HttpCredentials] = token
    val entity = HttpEntity(ContentTypes.`application/json`, write(Map(
      "addLabelIds" -> addLabelIds,
      "removeLabelIds" -> removeLabelIds)))
    val unmarshaller = Some(read[Message](_: String))
    val quotaUnits = 5
  }

  case class Send(message: Message, userId: String = "me")
    (implicit val token: OAuth2Identity) extends GmailRestRequest {
    assert(message.id == None)
    assert(message.labelIds == Nil)
    assert(message.snippet == None)
    assert(message.historyId == None)
    assert(message.payload == None)
    assert(message.sizeEstimate == None)
    assert(message.raw != None)

    val uri = s"$baseUri/users/$userId/messages/send"
    val method = HttpMethods.POST
    val credentials: Option[HttpCredentials] = token
    val entity: HttpEntity = HttpEntity(
      ContentTypes.`application/json`,
      write(message))
    val unmarshaller = Some(read[Message](_: String))
    val quotaUnits = 25
  }

  case class Trash(id: String, userId: String = "me")
    (implicit val token: OAuth2Identity) extends GmailRestRequest {

    val uri = s"$baseUri/users/$userId/messages/$id/trash"
    val method = HttpMethods.POST
    val credentials: Option[HttpCredentials] = token
    val entity = HttpEntity.Empty
    val unmarshaller = Some(read[Message](_: String))
    val quotaUnits = 5
  }

  case class Untrash(id: String, userId: String = "me")
    (implicit val token: OAuth2Identity) extends GmailRestRequest {

    val uri = s"$baseUri/users/$userId/messages/$id/untrash"
    val method = HttpMethods.POST
    val credentials: Option[HttpCredentials] = token
    val entity = HttpEntity.Empty
    val unmarshaller = Some(read[Message](_: String))
    val quotaUnits = 5
  }

  case class Import(message: Message, userId: String = "me")
    (implicit val token: OAuth2Identity) extends GmailRestRequest {
    assert(message.id == None)
    assert(message.labelIds == Nil)
    assert(message.snippet == None)
    assert(message.historyId == None)
    assert(message.payload == None)
    assert(message.sizeEstimate == None)
    assert(message.raw != None)

    val uri = s"$baseUri/users/$userId/messages/import"
    val method = HttpMethods.POST
    val credentials: Option[HttpCredentials] = token
    val entity: HttpEntity = HttpEntity(
      ContentTypes.`application/json`,
      write(message))
    val unmarshaller = Some(read[Message](_: String))
    val quotaUnits = 25
  }

  object Attachments {
    case class Get(id: String, messageId: String, userId: String = "me")
      (implicit val token: OAuth2Identity) extends GmailRestRequest {

      val uri = s"$baseUri/users/$userId/messages/$messageId/attachments/$id"
      val method = HttpMethods.GET
      val credentials: Option[HttpCredentials] = token
      val entity = HttpEntity.Empty
      val unmarshaller = Some(read[MessageAttachment](_: String))
      val quotaUnits = 5
    }
  }
}
