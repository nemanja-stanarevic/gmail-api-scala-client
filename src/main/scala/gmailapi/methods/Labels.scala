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
import gmailapi.resources.{ Label, GmailSerializer }
import org.json4s.jackson.Serialization.{ read, write }
import org.json4s.jackson.JsonMethods.parse
import scala.collection.immutable.Map
import scala.language.postfixOps
import spray.http.{ HttpCredentials, HttpEntity, HttpMethods, ContentTypes }

object Labels {
  import GmailSerializer._

  case class Create(label: Label, userId: String = "me")
    (implicit val token: OAuth2Identity) extends GmailRestRequest {

    val uri = s"$baseUri/users/$userId/labels"
    val method = HttpMethods.POST
    val credentials: Option[HttpCredentials] = token
    val entity: HttpEntity = HttpEntity(ContentTypes.`application/json`, write(label))
    val unmarshaller = Some(read[Label](_: String))
    val quotaUnits = 5
  }

  case class Delete(id: String, userId: String = "me")
    (implicit val token: OAuth2Identity) extends GmailRestRequest {

    val uri = s"$baseUri/users/$userId/labels/$id"
    val method = HttpMethods.DELETE
    val credentials: Option[HttpCredentials] = token
    val entity = HttpEntity.Empty
    val unmarshaller = None
    val quotaUnits = 5
  }

  case class List(userId: String = "me")
    (implicit val token: OAuth2Identity) extends GmailRestRequest {

    val uri = s"$baseUri/users/$userId/labels"
    val method = HttpMethods.GET
    val credentials: Option[HttpCredentials] = token
    val entity = HttpEntity.Empty
    val unmarshaller = Some((str: String) => (parse(str) \\ "labels").extract[Seq[Label]])
    val quotaUnits = 1
  }

  case class Update(id: String, label: Label, userId: String = "me")
    (implicit val token: OAuth2Identity) extends GmailRestRequest {

    val uri = s"$baseUri/users/$userId/labels/$id"
    val method = HttpMethods.PUT
    val credentials: Option[HttpCredentials] = token
    val entity = HttpEntity(ContentTypes.`application/json`, write(label))
    val unmarshaller = Some(read[Label](_: String))
    val quotaUnits = 5
  }

  case class Get(id: String, userId: String = "me")
    (implicit val token: OAuth2Identity) extends GmailRestRequest {

    val uri = s"$baseUri/users/$userId/labels/$id"
    val method = HttpMethods.GET
    val credentials: Option[HttpCredentials] = token
    val entity = HttpEntity.Empty
    val unmarshaller = Some(read[Label](_: String))
    val quotaUnits = 1
  }

  case class Patch(id: String, patch: Map[String, Any], userId: String = "me")
    (implicit val token: OAuth2Identity) extends GmailRestRequest {

    /* TO DO: Validate that patch keys are in Label resource. */
    val uri = s"$baseUri/users/$userId/labels/$id"
    val method = HttpMethods.PATCH
    val credentials: Option[HttpCredentials] = token
    val entity = HttpEntity(ContentTypes.`application/json`, write(patch))
    val unmarshaller = Some(read[Label](_: String))
    val quotaUnits = 5 /* undefined by API docs, but assuming 5 */
  }
}
