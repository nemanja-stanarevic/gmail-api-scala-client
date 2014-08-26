package gmailapi

import akka.actor.{ Actor, ActorRef, ActorSystem, OneForOneStrategy, Props }
import akka.event.LoggingReceive
import akka.pattern.{ ask, pipe }
import akka.testkit.{ TestProbe, ImplicitSender, TestKit }
import akka.util.Timeout
import org.json4s.jackson.Serialization.{read, write}
import org.json4s.JsonAST
import org.scalatest.{BeforeAndAfterAll, FlatSpec}
import org.scalatest.{FunSuite, FunSuiteLike}
import org.scalatest.matchers.ShouldMatchers
import com.typesafe.config.{Config, ConfigFactory}
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.collection.immutable.{Seq, List}
import scala.language.postfixOps
import scala.util.{Try, Success, Failure, Random}
import spray.http.{HttpRequest, HttpMethod, HttpMethods, HttpEntity, OAuth2BearerToken}
import spray.httpx.encoding.{Gzip, Deflate}
import spray.client.pipelining._
import com.typesafe.config._
import scala.util.Failure
import gmailapi.oauth2._
import gmailapi.methods._
import gmailapi.resources._
import gmailapi.restclient.RestResponses

class GmailLabelsSuite(_system: ActorSystem) 
  extends TestKit(_system) 
  with FunSuiteLike 
  with ShouldMatchers 
  with BeforeAndAfterAll 
  with ImplicitSender 
{
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
       }""").withFallback(ConfigFactory.load()) ))
  
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

  var actualLabel = Label(name = "foo") 
  var actualLabelId = ""
  test("02-Gmail.Labels.Create") {
    val probe = TestProbe()
    val id = scala.util.Random.nextLong
    val label = Label(
        name = "label-test-"+id, 
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

  test("03-Gmail.Labels.Get") {
    val probe = TestProbe()
    probe.send(gmailApi, Labels.Get(id = actualLabel.id.get))
    val result = probe.expectMsgType[Resource[Label]]
    val returnLabel = result.get
    
    if (actualLabel.id != returnLabel.id)
	  fail("Gmail.Labels.Get should return the requested label.")

	// now try a label that does not exist
    probe.send(gmailApi, Labels.Get(id = "Foo_Bar"))
    probe.expectMsg(NotFound)

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(250)
  }

  test("04-Gmail.Labels.Update") {
    val probe = TestProbe()
    val id = scala.util.Random.nextLong
    val newName = "label-test-"+id

    assert(newName != actualLabel.name)
  
    val newLabel = Label(
	  id = actualLabel.id,
	  name = newName,
	  messageListVisibility = MessageListVisibility.Show,
	  labelListVisibility = LabelListVisibility.LabelShow,
	  ownerType = actualLabel.ownerType)
	  
    probe.send(gmailApi, Labels.Update(id = newLabel.id.get, label = newLabel))
    val result = probe.expectMsgType[Resource[Label]]
    val returnLabel = result.get
    
    if (newLabel.id != returnLabel.id)
	  fail("Gmail.Labels.Update should preserve labelId.")

	if (newName != returnLabel.name)
	  fail("Gmail.Labels.Update should update label name.")
	  
	if (MessageListVisibility.Show != returnLabel.messageListVisibility)
	  fail("Gmail.Labels.Update should update message list visibility.")

	if (LabelListVisibility.LabelShow != returnLabel.labelListVisibility )
	  fail("Gmail.Labels.Update should update label list visibility.")
	  
	actualLabel = returnLabel
	
	// now try a label that does not exist
    probe.send(gmailApi, Labels.Update(id = "Foo_Bar", label = newLabel))
    probe.expectMsg(NotFound)

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(250)
  }

  test("05-Gmail.Labels.Patch") {
    val probe = TestProbe()
    val id = scala.util.Random.nextLong
    val newName = "label-test-"+id

    assert(newName != actualLabel.name)
  
    val patch = Map("name" -> newName)
	  
    probe.send(gmailApi, Labels.Patch(id = actualLabel.id.get, patch = patch))
    val result = probe.expectMsgType[Resource[Label]]
    val returnLabel = result.get
    
    if (actualLabel.id != returnLabel.id)
	  fail("Gmail.Labels.Patch should preserve labelId.")

	if (newName != returnLabel.name)
	  fail("Gmail.Labels.Patch should update label name.")
	  
	if (actualLabel.messageListVisibility != returnLabel.messageListVisibility)
	  fail("Gmail.Labels.Patch should preserve messageListVisibility.")

	if (actualLabel.labelListVisibility != returnLabel.labelListVisibility )
	  fail("Gmail.Labels.Patch should preserve labelListVisibility.")
	
	actualLabel = returnLabel
	
	// now try a label that does not exist
    probe.send(gmailApi, Labels.Patch(id = "Foo_Bar", patch = patch))
    probe.expectMsg(NotFound)

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(250)
  }
  
  test("06-Gmail.Labels.List") {
    val probe = TestProbe()
    probe.send(gmailApi, Labels.List())
    val result = probe.expectMsgType[Resource[Seq[Label]]]
    val returnLabelSeq = result.get

    if (returnLabelSeq filter (_.name == "INBOX") isEmpty)
	  fail("Gmail.Labels.List should include INBOX.")
    if (returnLabelSeq filter (_.name == "UNREAD") isEmpty)
	  fail("Gmail.Labels.List should include UNREAD.")
    if (returnLabelSeq filter (_.name == "SENT") isEmpty)
	  fail("Gmail.Labels.List should include SENT.")
    if (returnLabelSeq filter (_.name == actualLabel.name) isEmpty)
	  fail(s"Gmail.Labels.List should include $actualLabel.name.")

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(250)
  }  
 
  test("07-Gmail.Labels.Delete") {
    val probe = TestProbe()
    val id = actualLabel.id.get
    probe.send(gmailApi, Labels.Delete(id = id))
    probe.expectMsg(Done)

    probe.send(gmailApi, Labels.List())
    val result = probe.expectMsgType[Resource[Seq[Label]]]
    val returnLabelSeq = result.get

    if (! (returnLabelSeq filter (_.name == actualLabel.name) isEmpty) )
	  fail(s"Gmail.Labels.Delete should remove $actualLabel.name.")

	// now try a label that does not exist
    probe.send(gmailApi, Labels.Delete(id = "Foo_Bar"))
    probe.expectMsg(NotFound)

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(250)
  } 
}

