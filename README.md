# gmail-api-scala-client [![Build Status](https://travis-ci.org/nemanja-stanarevic/gmail-api-scala-client.svg?branch=master)](https://travis-ci.org/nemanja-stanarevic/gmail-api-scala-client)

## Motivation
Provide an asynchronous, actor-based, pure Scala library for Gmail API.

Google published Gmail API client libraries for Java, .NET and Python, with Go
and Dart libraries in the works. However, there is currently no native Scala
library and developing a wrapper around Google's Java library is unattractive
since the library is synchronous.

## High-Level Project Goals
* Fully asynchronous
* Actor-based
* Fast and lightweight
* Data model based on case classes and Scala collections
* Easily extensible to support additional Google and other third-party APIs
* Configuration via Typesafe Config
* Logging via Akka event bus
* Type safety

## Project Status

* Project is under development. Code documentation is under way.
* Pull requests, code reviews, comments and questions are all appreciated.

####Completed:
* OAuth, Labels, Messages, Threads, History, Attachments and Drafts APIs
* Test suites for all APIs
* Exponential back off wrapper for Gmail API actor, see `gmailapi.GmailApiActorBackoff`

####TO DOs:
* Scala Docs

####Future[TO DOs]:
* Full inbox sync example code
* Refactor with Google Discovery API and Scala reflection
* Refactor with Akka-Http
* Hide away nextPageToken from List methods for Threads, Messages and History

## Usage
Gmail API limits per user usage to 25 work units per second (moving average). For
more details, see <https://developers.google.com/gmail/api/v1/reference/quota>.

Clients should use `gmailapi.GmailApiActorBackoff`, which incorporates exponential
backoff, when there are too many requests for a specific user.

* Imports
```scala
import gmailapi.GmailApiActorBackoff
import gmailapi.oauth2._
import gmailapi.methods._
import gmailapi.resources._
import gmailapi.restclient.RestResponses
```

* OAuth2 methods take Typesafe Config as implicit parameter.  The config should
define `clientId`, `clientSecret`, `redirectUrl` and `scope`.  See 
<https://developers.google.com/gmail/api/auth/web-server> for more details.
```scala
val scope = Seq(
  "https://www.googleapis.com/auth/userinfo.email",
  "https://www.googleapis.com/auth/userinfo.profile",
  "https://mail.google.com/") mkString " "
implicit val oauthConfig = ConfigFactory.parseString(s"""
    | oauth2.clientId = "...",
    | oauth2.clientSecret = "...",
    | oauth2.redirectUri = "...",
    | oauth2.scope = "$scope" """.stripMargin)
```

* If the user is accessing the app for the first time (i.e. there is no
refresh token), run `gmailapi.oauth2.authorizationUri` function to get the
authorization Uri

* Create an actor and router as appropriate, for example
```scala
val props = Props(new GmailApiActorBackoff(maxRetries = 5))
val gmailApi = context.actorOf(props.withRouter(RoundRobinRouter(
  nrOfInstances = 10)))
```

* Exchange the authorization code received through `redirectUri` for an OAuth2
identity.  Store the result in an implicit var of `OAuth2Identity` type since 
api methods take an implicit `OAuth2Identity` parameter.
```scala
  gmailApi ! OAuth2.RequestToken(authCode)
```

* Invoke Gmail API methods
```scala
val label = Label(
  name = "My new label",
  messageListVisibility = MessageListVisibility.Hide,
  labelListVisibility = LabelListVisibility.LabelShowIfUnread)
gmailApi ! Labels.Create(label)

gmailApi ! Threads.List(labelIds = Seq("INBOX", "SENT"))

val rawMsg = MessageFactory.createMessage(
  fromAddress = Some(("Alice", "alice@gmail.com")),
  to = Seq(("Bob", "bob@gmail.com")),
  subject = Some("Hello"),
  textMsg = Some("World"),
  htmlMsg = Some(s"<html><body><i>World</i></body></html>"))
gmailApi ! Messages.Send(message = rawMsg)
```

## How to extend the API coverage

The package `gmailapi.restclient` includes traits and case classes for 
constructing clients for any REST service.

First, define resources that are produced/consumed by the REST service, as follows:

```scala
case class Person(
  userId: Option[String] = None,
  givenName: String,
  familyName: String,
  email: Option[String] = None,
  pictureUri: Option[String] = None)
```

Next, define serializers and deserializers for the resources. If REST
service is based on JSON format, use spray and json4s as follows:

```scala
import spray.httpx.Json4sJacksonSupport
import org.json4s.{ DefaultFormats, FieldSerializer, Formats }

object PersonSerializer extends Json4sJacksonSupport {
  implicit def json4sJacksonFormats : Formats = DefaultFormats +
  FieldSerializer[Person]() }
```

Next, define REST service methods as implementations of
`gmailapi.restclient.RestRequest` trait specifying concrete values for
abstract members `uri`, `method`, `credentials`, `entity`, and `unmarshaller`:

```scala
import org.json4s.jackson.Serialization.{ read, write }
import spray.http.{HttpCredentials, HttpEntity, HttpMethods, ContentTypes}

object People {
  import PersonSerializer._

  case class Create(person: Person)(implicit val token: HttpCredentials)
    extends RestRequest {
    val uri = s"https://api.myservice.com/people"
    val method = HttpMethods.POST
    val credentials = Some(token)
    val entity = HttpEntity(ContentTypes.`application/json`, write(person))
    val unmarshaller = Some(read[Person](_: String))

  case class Get(userId: String)(implicit val token: HttpCredentials)
    extends RestRequest {
    val uri = s"https://api.myservice.com/people/$userId"
    val method = HttpMethods.GET
    val credentials = Some(token)
    val entity = HttpEntity.Empty
    val unmarshaller = Some(read[Person](_: String))

  // other REST methods defined...
}
```

Finally, define a concrete `Actor` for the REST service by implementing 
`RestActor` trait.

```scala
class PeopleApiActor extends Actor with RestActor {
  // define a logger
  val log = Logging(context.system, this)

  // define error handlers
  val errorHandler = List(
    RestActor.ErrorHandler(
      // when http response is...
      StatusCodes.Unauthorized,
      // reply with...
      RestResponses.ExpiredAuthToken) {
        // when this condition is satisfied
        _.contains("Expired Authentication Token")
      },
      // other error handlers...
    )
}
```

Access the APIs as follows:

```scala
val myApi = system.actorOf(Props(new PeopleApiActor))
implicit val creds = OAuth2BearerToken("...")

val person = Person(
  givenName = "John",
  familyName = "Doe",
  email = Some("john.doe@gmail.com"))

myApi ! People.Create(person)
```

## Dependencies

* Scala 2.11 <https://typesafe.com/platform/tools/scala>
* Akka 2.3.4 <https://typesafe.com/platform/runtime/akka>
* Spray <http://spray.io>
* json4s <http://json4s.org>
* scalatest <http://http://www.scalatest.org>

## Installation

You can add the gmail-api-scala-client as a dependency as follows:

### SBT

```scala
  // add Typesafe & Sonatype Snapshot resolvers
  resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",

  // add library dependency
  libraryDependencies ++= {
    val gmailApiScalaClientVersion = "0.1.0-SNAPSHOT"
    Seq("com.github.nemanja-stanarevic" %% "gmail-api-scala-client" % gmailApiScalaClientVersion)
  }
```

Made with ❤ in NYC at Hacker School <http://hackerschool.com>
