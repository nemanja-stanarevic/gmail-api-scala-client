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

class GmailHistorySuite(_system: ActorSystem)
  extends TestKit(_system)
  with FunSuiteLike
  with ShouldMatchers
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

  var actualMessageId = ""
  var startingHistoryId = ""

  test("02-Gmail.Messages.Insert -- Start of history observation") {
    val probe = TestProbe()
    val id = scala.util.Random.nextLong
    val actualSubject = "Scala API Test " + id
    val actualTo = s"scala.api.test+$id@gmail.com"
    val rawMsg = MessageFactory.createMessage(
      labelIds = Seq("INBOX"),
      fromAddress = Some(("Scala API Test", "scala.api.test@gmail.com")),
      to = Seq(("Scala API Test", actualTo)),
      subject = Some(actualSubject),
      textMsg = Some(actualSubject),
      htmlMsg = Some(s"<html><body><i>$actualSubject</i></body></html>"))
    probe.send(gmailApi, Messages.Insert(message = rawMsg))
    val result = probe.expectMsgType[Resource[Message]]
    val actualMessage = result.get
    actualMessageId = actualMessage.id.get

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(1000)

    // However, we need to "get" a message to see historyId - 
    probe.send(gmailApi, Messages.Get(id = actualMessageId))
    val fullResult = probe.expectMsgType[Resource[Message]]
    val fullMessage = fullResult.get
    startingHistoryId = fullMessage.historyId.get

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(250)
  }

  test("03-Gmail.Histories.List") {
    val probe = TestProbe()
    probe.send(gmailApi, Histories.List(
      startHistoryId = startingHistoryId,
      labelIds = Seq("INBOX"),
      maxResults = Some(1000)))
    val result = probe.expectMsgType[Resource[HistoryList]]
    val returnHistoryList = result.get

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(250)
  }

  var additionalMessageIds = List[String]()
  test("04-Gmail.Messages.Insert -- insert 5 new messages") {
    val probe = TestProbe()
    var count = 5
    while (count > 0) {
      val id = scala.util.Random.nextLong
      val actualSubject = "Scala API Test " + id
      val actualTo = s"scala.api.test+$id@gmail.com"
      val rawMsg = MessageFactory.createMessage(
        labelIds = Seq("INBOX"),
        fromAddress = Some(("Scala API Test", "scala.api.test@gmail.com")),
        to = Seq(("Scala API Test", actualTo)),
        subject = Some(actualSubject),
        textMsg = Some(actualSubject),
        htmlMsg = Some(s"<html><body><i>$actualSubject</i></body></html>"))
      probe.send(gmailApi, Messages.Insert(message = rawMsg))
      val result = probe.expectMsgType[Resource[Message]]
      val actualMessage = result.get
      additionalMessageIds = actualMessage.id.get :: additionalMessageIds
      // this is to throttle the request rate on Google API
      java.lang.Thread.sleep(1000)
      count = count - 1
    }
  }

  test("05-Gmail.Histories.List -- confirm new messages are in the history") {
    val probe = TestProbe()
    probe.send(gmailApi, Histories.List(
      startHistoryId = startingHistoryId,
      labelIds = Seq("INBOX"),
      maxResults = Some(1000)))
    val result = probe.expectMsgType[Resource[HistoryList]]
    val returnHistoryList = result.get

    // confirm that startingHistoryId is no longer the current history id
    if (returnHistoryList.historyId == startingHistoryId)
      fail(s"Gmail.Histories.List should not return a stale history Id")

    if (returnHistoryList.history == Nil)
      fail(s"Gmail.Histories.List should return a non-empty history list.")

    val messages = returnHistoryList.history flatMap (_.messages)
    additionalMessageIds foreach { messageId =>
      if (messages.find(_.id.get == messageId) == None)
        fail(s"Gmail.Histories.List should include messageId: $messageId.")
    }

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(250)
  }

  test("06-Gmail.Messages.Delete -- Cleanup") {
    val probe = TestProbe()
    assert(actualMessageId != "")
    probe.send(gmailApi, Messages.Delete(id = actualMessageId))
    probe.expectMsg(Done)

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(500)

    additionalMessageIds foreach { messageId =>
      probe.send(gmailApi, Messages.Delete(id = messageId))
      probe.expectMsg(Done)
      // this is to throttle the request rate on Google API
      java.lang.Thread.sleep(500)
    }
  }
}

