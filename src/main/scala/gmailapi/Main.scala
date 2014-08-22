package gmailapi

import akka.actor.{ Actor, ActorRef, ActorSystem, OneForOneStrategy, Props }
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import org.json4s.jackson.Serialization.{read, write}
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.collection.immutable.{Seq, List}
import scala.language.postfixOps
import scala.util.{Try, Success, Failure}
import spray.http.{HttpRequest, HttpMethod, HttpMethods, HttpEntity, OAuth2BearerToken}
import spray.httpx.encoding.{Gzip, Deflate}
import spray.client.pipelining._
import spray.http.OAuth2BearerToken
import akka.testkit.TestProbe
import gmailapi._
import gmailapi.oauth2._
import gmailapi.methods.Labels
import gmailapi.resources.Label


object Main extends App {
  val config: Config = ConfigFactory.parseString("""akka {
          loglevel = "DEBUG"
            actor {
              debug {
                receive = on
                lifecycle = off
              }
            }
          }""").withFallback(ConfigFactory.load())
  implicit val system = ActorSystem("TestSystem", config)
  implicit val timeout: Timeout = Timeout(15 seconds)
  import system.dispatcher // implicit execution context

  val scope = Seq(
      "https://www.googleapis.com/auth/userinfo.email",
      "https://www.googleapis.com/auth/userinfo.profile",
      "https://mail.google.com/") mkString " "

  implicit val oauthConfig = ConfigFactory.parseString(s"""
      | oauth2.authUri = "https://accounts.google.com/o/oauth2/auth",
      | oauth2.tokenUri = "https://accounts.google.com/o/oauth2/token",
	  | oauth2.validationUri = "https://www.googleapis.com/oauth2/v1/tokeninfo",
      | oauth2.userInfoUri = "https://www.googleapis.com/oauth2/v1/userinfo",
      | oauth2.clientId = "180808591279-pgbqlvuh51u6gbua2t9bqssteqcf3bqc.apps.googleusercontent.com",
      | oauth2.clientSecret = "rrWmKHyI6RzCyAEvc64nXl97",
      | oauth2.redirectUri = "https://api.tagmail.io/api/v1/g-oauth2-callback",
      | oauth2.scope = "$scope" """.stripMargin)
    
    import gmailapi.restclient._
    
    val probe = TestProbe()
    implicit val token = OAuth2Identity(
        accessToken = "ya29.ZwDqqseEVmBpQiAAAAAjNwNeBVz4yGFzdnYMlkc6xeRxUttK0uDux-kbLaAKFQ",
        refreshToken = "1/AuPkolUn76aoScvfW-7tHj16XanQvX7tY28x4my1Pz8",
        expiration = 0)
        
    val gmailApi = system.actorOf(Props(new GmailApiActor))

    probe.send(gmailApi, Labels.Create(Label(name = "my foo bar label 9")))
    
    val result = probe.expectMsgType[RestResponses.Resource[Label]](5 seconds)
    println(result.get)
    
    readLine()
  system.shutdown()
}