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

class GmailMessagesSuite(_system: ActorSystem) 
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
  
  override def afterAll = 
    system.shutdown()

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

  var actualMessage = Message(threadId="Foo_Bar") 
  var actualMessageId = ""
  
  /* TO DO: These are toy tests, needs more work once the Messages framework is completed */
  test("01-Gmail.Messages.List") {
    val probe = TestProbe()
    probe.send(gmailApi, Messages.List())
    val result = probe.expectMsgType[Resource[MessageList]]
    val returnMessageList = result.get

    // get the id of the last message
    returnMessageList.messages foreach { message =>
      actualMessageId = message.id.get
    }
  }  
  test("02-Gmail.Messages.Get") {
    val probe = TestProbe()
    assert(actualMessageId != "")
    probe.send(gmailApi, Messages.Get(id = actualMessageId))
    val result = probe.expectMsgType[Resource[Message]]
    val returnMessage = result.get
    
    if (actualMessageId != returnMessage.id.get)
	  fail("Gmail.Messages.Get should return the requested message.")
	
    if (returnMessage.id == None)
	  fail("Gmail.Messages.Get should include historyId.")

    if (returnMessage.threadId  == "")
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
	  	  
	// now try a message that does not exist
    probe.send(gmailApi, Messages.Get(id = "Foo_Bar"))
    probe.expectMsg(NotFound)
  }

  
  test("03-Gmail.Messages.Delete") {
    val probe = TestProbe()
    assert(actualMessageId != "")
    probe.send(gmailApi, Messages.Delete(id = actualMessageId))
    probe.expectMsg(Done)

    probe.send(gmailApi, Messages.List())
    val result = probe.expectMsgType[Resource[MessageList]]
    val returnMessageList = result.get

    if (! (returnMessageList.messages filter (_.id == actualMessageId) isEmpty) )
	  fail(s"Gmail.Messages.Delete should remove $actualMessageId.")

	// now try a label that does not exist
    probe.send(gmailApi, Messages.Delete(id = "Foo_Bar"))
    probe.expectMsg(NotFound)
  } 
}

