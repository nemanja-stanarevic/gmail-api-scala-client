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

class GmailThreadsSuite(_system: ActorSystem)
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

  var actualMessage = Message()
  var actualMessageId = ""
  var actualThreadId = ""

  test("02-Gmail.Messages.Insert [to get a thread reference]") {
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
    actualMessage = result.get
    actualMessageId = actualMessage.id.get
    actualThreadId = actualMessage.threadId.get

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(1000)
  }

  test("03-Gmail.Threads.Get") {
    val probe = TestProbe()
    probe.send(gmailApi, Threads.Get(id = actualThreadId))
    val result = probe.expectMsgType[Resource[Thread]]
    val retrunThread = result.get

    // confirm that inserted message is in the list
    if (retrunThread.messages filter (_.id == Some(actualMessageId)) isEmpty)
      fail(s"Gmail.Threads.Get should include a message with id: $actualMessageId.")

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(250)
  }

  test("04-Gmail.Threads.List") {
    val probe = TestProbe()
    probe.send(gmailApi, Threads.List())
    val result = probe.expectMsgType[Resource[ThreadList]]
    val returnThreadList = result.get

    // confirm that inserted message is in the list
    if (returnThreadList.threads filter (_.id == actualThreadId) isEmpty)
      fail(s"Gmail.Threads.List should include a thread with id: $actualThreadId.")

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(250)
  }

  var actualLabel = Label(name = "foo")
  var actualLabelId = ""

  test("05-Gmail.Labels.Create") {
    val probe = TestProbe()
    val id = scala.util.Random.nextLong
    val label = Label(
      name = "label-test-" + id,
      messageListVisibility = MessageListVisibility.Hide,
      labelListVisibility = LabelListVisibility.LabelShowIfUnread)
    probe.send(gmailApi, Labels.Create(label))
    val result = probe.expectMsgType[Resource[Label]]
    actualLabel = result.get

    if (actualLabel.id == None)
      fail("Gmail.Labels.Create should set the label id.")
    actualLabelId = actualLabel.id.get

    if (actualLabel.name != label.name)
      fail("Gmail.Labels.Create should set the label name.")

    if (actualLabel.messageListVisibility != label.messageListVisibility)
      fail("Gmail.Labels.Create should set messageListVisibility.")

    if (actualLabel.labelListVisibility != label.labelListVisibility)
      fail("Gmail.Labels.Create should set labelListVisibility.")

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(250)
  }

  test("06-Gmail.Threads.Modify--AddLabel"){
    val probe = TestProbe()
    probe.send(gmailApi, Threads.Modify(
        id = actualThreadId,
        addLabelIds = Seq(actualLabelId)))
    val result = probe.expectMsgType[Resource[Thread]]
    val resultingThread = result.get

    // check message within the thread
    assert(actualMessageId != "")
    probe.send(gmailApi, Messages.Get(id = actualMessageId))
    val returnMessage = probe.expectMsgType[Resource[Message]].get

    if (!returnMessage.labelIds.contains(actualLabelId))
      fail("Gmail.Threads.Modify should add a label to the message.")

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(250)
  }

  test("07-Gmail.Threads.Modify--RemoveLabel"){
    val probe = TestProbe()
    probe.send(gmailApi, Threads.Modify(
        id = actualThreadId,
        removeLabelIds = Seq(actualLabelId)))
    val result = probe.expectMsgType[Resource[Thread]]
    val resultingThread = result.get

    // check message within the thread
    assert(actualMessageId != "")
    probe.send(gmailApi, Messages.Get(id = actualMessageId))
    val returnMessage = probe.expectMsgType[Resource[Message]].get

    if (returnMessage.labelIds contains actualLabelId)
      fail("Gmail.Threads.Modify should remove a label from the message.")

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(250)
  }

  test("08-Gmail.Labels.Delete") {
    val probe = TestProbe()
    probe.send(gmailApi, Labels.Delete(id = actualLabelId))
    probe.expectMsg(Done)
    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(250)
  }

  test("09-Gmail.Threads.Trash"){
    val probe = TestProbe()
    probe.send(gmailApi, Threads.Trash(id = actualThreadId))
    val result = probe.expectMsgType[Resource[Thread]]
    val resultingThread = result.get

    // check message within the thread
    assert(actualMessageId != "")
    probe.send(gmailApi, Messages.Get(id = actualMessageId))
    val returnMessage = probe.expectMsgType[Resource[Message]].get

    if (!returnMessage.labelIds.contains("TRASH"))
      fail("Gmail.Threads.Trash should apply TRASH label.")

    if (returnMessage.labelIds.contains("INBOX"))
      fail("Gmail.Threads.Trash should remove INBOX label.")

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(250)
  }

  test("10-Gmail.Threads.Untrash"){
    val probe = TestProbe()
    probe.send(gmailApi, Threads.Untrash(id = actualThreadId))
    val result = probe.expectMsgType[Resource[Thread]]
    val resultingThread = result.get

    // check message within the thread
    assert(actualMessageId != "")
    probe.send(gmailApi, Messages.Get(id = actualMessageId))
    val returnMessage = probe.expectMsgType[Resource[Message]].get

    if (returnMessage.labelIds.contains("TRASH"))
      fail("Gmail.Threads.Untrash should remove TRASH label.")

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(250)
  }


  test("11-Gmail.Threads.Delete") {
    val probe = TestProbe()
    assert(actualThreadId != "")
    probe.send(gmailApi, Threads.Delete(id = actualThreadId))
    probe.expectMsg(Done)

    probe.send(gmailApi, Messages.List())
    val result = probe.expectMsgType[Resource[MessageList]]
    val returnMessageList = result.get

    if (!(returnMessageList.messages filter (_.threadId == Some(actualThreadId)) isEmpty))
      fail(s"Gmail.Threads.Delete should remove all messages under $actualThreadId thread.")

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(1000)

    // now try a thread that does not exist
    probe.send(gmailApi, Threads.Delete(id = "Foo_Bar"))
    probe.expectMsg(NotFound)

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(1000)
  }
}

