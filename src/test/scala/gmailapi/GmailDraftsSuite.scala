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

import akka.actor.{ Actor, ActorRef, ActorSystem, OneForOneStrategy, Props }
import akka.event.LoggingReceive
import akka.pattern.{ ask, pipe }
import akka.testkit.{ TestProbe, ImplicitSender, TestKit }
import akka.util.Timeout
import org.json4s.jackson.Serialization.{ read, write }
import org.json4s.JsonAST
import org.scalatest.{ BeforeAndAfterAll, FlatSpec }
import org.scalatest.{ FunSuite, FunSuiteLike }
import org.scalatest.matchers.ShouldMatchers
import com.typesafe.config.{ Config, ConfigFactory }
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.collection.immutable.{ Seq, List }
import scala.language.postfixOps
import scala.util.{ Try, Success, Failure, Random }
import spray.http.{ HttpRequest, HttpMethod, HttpMethods, HttpEntity, OAuth2BearerToken }
import spray.httpx.encoding.{ Gzip, Deflate }
import spray.client.pipelining._
import com.typesafe.config._
import scala.util.Failure
import gmailapi.oauth2._
import gmailapi.methods._
import gmailapi.resources._
import gmailapi.restclient.RestResponses

class GmailDraftsSuite(_system: ActorSystem)
  extends TestKit(_system)
  with FunSuiteLike
  with BeforeAndAfterAll
  with ImplicitSender {
  def this() = this(ActorSystem("TestSystem", ConfigFactory.parseString("""
      akka {
         log-dead-letters-during-shutdown = false
         loglevel = "INFO"
         actor {
           debug {
             receive = off
             lifecycle = off
           }
         }
       }""").withFallback(ConfigFactory.load())))

  override def afterAll = {
    system.shutdown()
  }

  val scope = Seq(
    "https://www.googleapis.com/auth/userinfo.email",
    "https://www.googleapis.com/auth/userinfo.profile",
    "https://mail.google.com/") mkString " "

  implicit val oauthConfig = ConfigFactory.parseString(s"""
      | oauth2.clientId = "453782910822-gi5h9s2qpsi9kkafkqbt7pdk4ak1srpe.apps.googleusercontent.com",
      | oauth2.clientSecret = "3gV-C_UH7UJLOtn2wrpXpnrk",
      | oauth2.redirectUri = "urn:ietf:wg:oauth:2.0:oob",
      | oauth2.scope = "$scope" """.stripMargin)

  import gmailapi.restclient.RestResponses._
  import system.dispatcher

  implicit val timeout: Timeout = Timeout(10 seconds)
  implicit var oauthId = OAuth2Identity(
    accessToken = "ya29.aADupUf-0tqZViEAAAA9IotnLNyPCzgWl30u7teSVJngNwWwhjYd7AHbHngmUWJZzUvs29Q3q2kXV19dwkQ",
    refreshToken = "1/9RdQbLY2ATZu9EctSZKWgRSQaVRx6-LP1zfdV6kWPds",
    expiration = 1408641363)

  val gmailApi = system.actorOf(Props(new GmailApiActor))

  test("01-Gmail.OAuth-RefreshToken") {
    val probe = TestProbe()
    probe.send(gmailApi, OAuth2.RefreshToken(oauthId))
    val result = probe.expectMsgType[Resource[OAuth2Identity]]
    oauthId = result.get
  }

  var actualDraft = Draft()
  var actualDraftId = ""
  var actualSubject = ""
  var actualTo = ""

  test("02-Gmail.Drafts.Create") {
    val probe = TestProbe()
    val id = scala.util.Random.nextLong
    actualSubject = "Scala API Test " + id
    actualTo = s"scala.api.test+$id@gmail.com"
    val rawMsg = MessageFactory.createMessage(
      fromAddress = Some(("Scala API Test", "scala.api.test@gmail.com")),
      to = Seq(("Scala API Test", actualTo)),
      subject = Some(actualSubject),
      textMsg = Some(actualSubject),
      htmlMsg = Some(s"<html><body><i>$actualSubject</i></body></html>"))
    probe.send(gmailApi, Drafts.Create(Draft(message = Some(rawMsg))))
    val result = probe.expectMsgType[Resource[Draft]]
    actualDraft = result.get
    actualDraftId = actualDraft.id.get

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(1000)
  }

  test("03-Gmail.Drafts.List") {
    val probe = TestProbe()
    probe.send(gmailApi, Drafts.List())
    val result = probe.expectMsgType[Resource[DraftList]]
    val returnDraftList = result.get

    // confirm that inserted message is in the list
    if (returnDraftList.drafts filter (_.id == Some(actualDraftId)) isEmpty)
      fail(s"Gmail.Drafts.List should include a message with id: $actualDraftId.")

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(250)
  }

  test("04-Gmail.Messages.Get") {
    val probe = TestProbe()
    assert(actualDraftId != "")
    probe.send(gmailApi, Drafts.Get(id = actualDraftId))
    val result = probe.expectMsgType[Resource[Draft]]
    val returnDraft = result.get

    if (actualDraftId != returnDraft.id.get)
      fail("Gmail.Drafts.Get should return the requested message.")

    if (returnDraft.message.get.id == None)
      fail("Gmail.Drafts.Get should include historyId.")

    if (returnDraft.message.get.threadId == "")
      fail("Gmail.Drafts.Get should include threadId.")

    if (returnDraft.message.get.labelIds.length == 0)
      fail("Gmail.Drafts.Get should include some labelIds.")

    if (returnDraft.message.get.snippet == None)
      fail("Gmail.Drafts.Get should include snippet.")

    if (returnDraft.message.get.historyId == None)
      fail("Gmail.Drafts.Get should include historyId.")

    if (returnDraft.message.get.payload == None)
      fail("Gmail.Drafts.Get should include payload.")

    if (returnDraft.message.get.sizeEstimate == None)
      fail("Gmail.Drafts.Get should include sizeEstimate.")

    if (returnDraft.message.get.raw != None)
      fail("Gmail.Drafts.Get should not include raw (this was full request).")

    // now try a message that does not exist
    probe.send(gmailApi, Drafts.Get(id = "Foo_Bar"))
    probe.expectMsg(NotFound)

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(250)
  }

  test("05-Gmail.Drafts.Update") {
    val probe = TestProbe()
    val id = scala.util.Random.nextLong
    actualSubject = "Scala API Test " + id
    actualTo = s"scala.api.test+$id@gmail.com"
    val rawMsg = MessageFactory.createMessage(
      fromAddress = Some(("Scala API Test", "scala.api.test@gmail.com")),
      to = Seq(("Scala API Test", actualTo)),
      subject = Some(actualSubject),
      textMsg = Some(actualSubject),
      htmlMsg = Some(s"<html><body><i>$actualSubject</i></body></html>"))
    probe.send(gmailApi, Drafts.Update(id = actualDraftId, draft = actualDraft.updated(rawMsg)))
    // don't care about the result here, b/c it's incomplete message
    probe.expectMsgType[Resource[Draft]]

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(1000)

    probe.send(gmailApi, Drafts.Get(id = actualDraftId))
    val result = probe.expectMsgType[Resource[Draft]]
    actualDraft = result.get

    if (actualDraft.message.get.payload.get.headers filter { header =>
      header.name == "Subject" & header.value == actualSubject
    } isEmpty)
      fail("Gmail.Drafts.Update should update the message.")

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(250)
  }

  var actualMessageId = ""
  test("06-Gmail.Drafts.Send"){
    val probe = TestProbe()
    probe.send(gmailApi, Drafts.Send(Draft(id = Some(actualDraftId))))
    val result = probe.expectMsgType[Resource[Message]]
    val resultingMessage = result.get

    if (!resultingMessage.labelIds.contains("SENT"))
      fail("Gmail.Drafts.Send should apply SENT label.")

    actualMessageId = resultingMessage.id.get

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(1000)
  }

  test("07-Gmail.Drafts.Delete") {
    // have to create a new draft here - 
    val probe = TestProbe()
    val id = scala.util.Random.nextLong
    actualSubject = "Scala API Test " + id
    actualTo = s"scala.api.test+$id@gmail.com"
    val rawMsg = MessageFactory.createMessage(
      fromAddress = Some(("Scala API Test", "scala.api.test@gmail.com")),
      to = Seq(("Scala API Test", actualTo)),
      subject = Some(actualSubject),
      textMsg = Some(actualSubject),
      htmlMsg = Some(s"<html><body><i>$actualSubject</i></body></html>"))
    probe.send(gmailApi, Drafts.Create(Draft(message = Some(rawMsg))))
    val result = probe.expectMsgType[Resource[Draft]]
    actualDraft = result.get
    actualDraftId = actualDraft.id.get

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(1000)

    probe.send(gmailApi, Drafts.Delete(id = actualDraftId))
    probe.expectMsg(Done)

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(1000)

    probe.send(gmailApi, Drafts.List())
    val listResult = probe.expectMsgType[Resource[DraftList]]
    val returnDraftList = listResult.get

    // confirm that inserted message is not in the list
    if (! (returnDraftList.drafts filter (_.id == Some(actualDraftId)) isEmpty) )
      fail(s"Gmail.Drafts.Delete should remove a draft with id: $actualDraftId.")

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(1000)

    // now try a label that does not exist
    probe.send(gmailApi, Drafts.Delete(id = "Foo_Bar"))
    probe.expectMsg(NotFound)

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(1000)
  }
}

