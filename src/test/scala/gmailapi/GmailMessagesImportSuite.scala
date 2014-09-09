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

class GmailMessagesImportSuite(_system: ActorSystem)
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

  var secondMessage = Message()
  var secondMessageId = ""
  var secondSubject = ""
  var secondTo = ""
  test("02-Gmail.Messages.Import") {
    val probe = TestProbe()
    val id = scala.util.Random.nextLong
    secondSubject = "Scala API Test " + id
    secondTo = s"scala.api.test+$id@gmail.com"
    val rawMsg = MessageFactory.createMessage(
      fromAddress = Some(("Scala API Test", "scala.api.test@gmail.com")),
      to = Seq(("Scala API Test", secondTo)),
      subject = Some(secondSubject),
      textMsg = Some(secondSubject),
      htmlMsg = Some(s"<html><body><i>$secondSubject</i></body></html>"))
    probe.send(gmailApi, Messages.Import(message = rawMsg))
    /* not sure why we need 60 s timeout here when the roundtrip takes ~0.5 s*/
    val result = probe.expectMsgType[Resource[Message]](60 seconds)
    secondMessage = result.get
    secondMessageId = secondMessage.id.get

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(1000)
  }

  test("03-Gmail.Messages.Get--ImportedMessage") {
    val probe = TestProbe()
    assert(secondMessageId != "")
    probe.send(gmailApi, Messages.Get(id = secondMessageId))
    val result = probe.expectMsgType[Resource[Message]]
    val returnMessage = result.get

    if (secondMessageId != returnMessage.id.get)
      fail("Gmail.Messages.Get should return the requested message.")

    if (returnMessage.id == None)
      fail("Gmail.Messages.Get should include historyId.")

    if (returnMessage.threadId == "")
      fail("Gmail.Messages.Get should include threadId.")

    if (returnMessage.labelIds.length == 0)
      fail("Gmail.Messages.Get should include some labelIds.")

    if (returnMessage.snippet == None)
      fail("Gmail.Messages.Get should include snippet.")

    if (returnMessage.historyId == None)
      fail("Gmail.Messages.Get should include historyId.")

    if (returnMessage.payload == None)
      fail("Gmail.Messages.Get should include payload.")

    if (returnMessage.sizeEstimate == None)
      fail("Gmail.Messages.Get should include sizeEstimate.")

    if (returnMessage.raw != None)
      fail("Gmail.Messages.Get should not include raw (this was full request).")

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(250)
  }

  test("04-Gmail.Messages.Delete") {
    val probe = TestProbe()

    assert(secondMessageId != "")
    probe.send(gmailApi, Messages.Delete(id = secondMessageId))
    probe.expectMsg(Done)
    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(500)


    probe.send(gmailApi, Messages.List())
    val result = probe.expectMsgType[Resource[MessageList]]
    val returnMessageList = result.get

    if (!(returnMessageList.messages filter (_.id == secondMessageId) isEmpty))
      fail(s"Gmail.Messages.Delete should remove $secondMessageId.")

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(250)

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(500)
  }
}

