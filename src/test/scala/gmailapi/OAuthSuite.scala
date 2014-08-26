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
import gmailapi.restclient.RestResponses

class OAuthSuite(_system: ActorSystem) 
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

  test("01-OAuth2-ValidateExpiredToken") {
    val probe = TestProbe()
    probe.send(gmailApi, OAuth2.ValidateToken(oauthId))
    probe.expectMsg(ExpiredAuthToken)

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(100)
  }
  
  test("02-OAuth2-FlagInvalidToken") {
    val probe = TestProbe()
    val invalidAuthId = OAuth2Identity(
    	accessToken = "foo",
    	refreshToken = "bar",
    	expiration = 1408641363)
    probe.send(gmailApi, OAuth2.ValidateToken(invalidAuthId))
    // depending on the uri, google will return 400 or 401 when token is either expired
    // or invalid, so we will pass along ExpiredAuthToken message to allow client to 
    // to attempt a refresh
    probe.expectMsg(ExpiredAuthToken)

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(100)
  }
  
  test("03-OAuth2-RefreshToken") {
    val probe = TestProbe()
    probe.send(gmailApi, OAuth2.RefreshToken(oauthId))
    val result = probe.expectMsgType[Resource[OAuth2Identity]]
    oauthId = result.get 

	if (oauthId.expiration <= (System.currentTimeMillis() / 1000))
	  fail("OAuth2.RefreshToken should issue a new non-expired token.")
	if (oauthId.expiration <= (System.currentTimeMillis() / 1000 + 3590))
	  fail("OAuth2.RefreshToken should issue a token that is good for ~1 hour.")

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(100)
  }
  
  test("04-OAuth2-ValidateValidToken") {
    val probe = TestProbe()
    probe.send(gmailApi, OAuth2.ValidateToken(oauthId))
    val result = probe.expectMsgType[Resource[OAuth2Identity]]
    oauthId = result.get 
        
    if (oauthId.email == None | 
        oauthId.userId == None |
        oauthId.scope == Nil)
      fail("OAuth2.ValidateToken should set email, user_id and scope.")

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(100)
  }
  
  test("05-OAuth2-GetUserInfo") {
    val probe = TestProbe()
    oauthId = OAuth2Identity(
        accessToken = oauthId.accessToken, 
        refreshToken = oauthId.refreshToken, 
        expiration = oauthId.expiration,
        scope = oauthId.scope)
    val oldAuthId = oauthId
    probe.send(gmailApi, OAuth2.GetUserInfo(oauthId))
    val result = probe.expectMsgType[Resource[OAuth2Identity]]
    oauthId = result.get 
    
    if (oauthId.email == None)
      fail("OAuth2.GetUserInfo should set email.")
    if (oauthId.userId == None)
      fail("OAuth2.GetUserInfo should set userId.")
    if (oauthId.name == None)
      fail("OAuth2.GetUserInfo should set name.")
    if (oauthId.givenName == None)
      fail("OAuth2.GetUserInfo should set givenName.")
    if (oauthId.familyName == None)
      fail("OAuth2.GetUserInfo should set familyName.")
    if (oauthId.picture == None)
      fail("OAuth2.GetUserInfo should set picture.")
    if (oauthId.gender == None)
      fail("OAuth2.GetUserInfo should set gender.")
    if (oauthId.locale == None) 
      fail("OAuth2.GetUserInfo should set locale.")
    if (oldAuthId.scope != oauthId.scope)
      fail("OAuth2.GetUserInfo should not change scope.")

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(100)
  }
  
  test("06-OAuth2-RefreshTokenLeavesInfoAlone") {
    val probe = TestProbe()
    val oldAuthId = oauthId
    probe.send(gmailApi, OAuth2.RefreshToken(oauthId))
    val result = probe.expectMsgType[Resource[OAuth2Identity]]
    oauthId = result.get 

    if (oldAuthId.refreshToken != oauthId.refreshToken)
      fail("Oauth2.RefreshToken should not change the value of refresh token.")
    if (oldAuthId.email != oauthId.email)
      fail("OAuth2.RefreshToken should not change email.")
    if (oldAuthId.familyName != oauthId.familyName)
      fail("OAuth2.RefreshToken should not change familyName.")
    if (oldAuthId.gender != oauthId.gender)
      fail("OAuth2.RefreshToken should not change gender.")
    if (oldAuthId.givenName != oauthId.givenName)
      fail("OAuth2.RefreshToken should not change givenName.")
    if (oldAuthId.locale != oauthId.locale)
      fail("OAuth2.RefreshToken should not change locale.")
    if (oldAuthId.name  != oauthId.name)
      fail("OAuth2.RefreshToken should not change name.")
    if (oldAuthId.picture  != oauthId.picture)
      fail("OAuth2.RefreshToken should not change picture.")
    if (oldAuthId.scope != oauthId.scope)
      fail("OAuth2.RefreshToken should not change scope.")
    if (oldAuthId.userId  != oauthId.userId)
      fail("OAuth2.RefreshToken should not change userId.")

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(100)
  }

  test("07-OAuth2-ValidateTokenLeavesInfoAlone") {
    val probe = TestProbe()
    val oldAuthId = oauthId
    probe.send(gmailApi, OAuth2.ValidateToken(oauthId))
    val result = probe.expectMsgType[Resource[OAuth2Identity]]
    oauthId = result.get 

    if (oldAuthId.refreshToken != oauthId.refreshToken)
      fail("Oauth2.ValidateToken should not change the value of refresh token.")
    if (oldAuthId.familyName != oauthId.familyName)
      fail("OAuth2.ValidateToken should not change familyName.")
    if (oldAuthId.gender != oauthId.gender)
      fail("OAuth2.ValidateToken should not change gender.")
    if (oldAuthId.givenName != oauthId.givenName)
      fail("OAuth2.RefreshToken should not change givenName.")
    if (oldAuthId.locale != oauthId.locale)
      fail("OAuth2.ValidateToken should not change locale.")
    if (oldAuthId.name  != oauthId.name)
      fail("OAuth2.ValidateToken should not change name.")
    if (oldAuthId.picture  != oauthId.picture)
      fail("OAuth2.ValidateToken should not change picture.")

    // this is to throttle the request rate on Google API
    java.lang.Thread.sleep(100)
  }
}

