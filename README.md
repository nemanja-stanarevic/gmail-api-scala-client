# gmail-api-scala-client [![Build Status](https://travis-ci.org/nemanja-stanarevic/gmail-api-scala-client.svg?branch=master)](https://travis-ci.org/nemanja-stanarevic/gmail-api-scala-client)

## Motivation
Google published Gmail API client libraries for Java, .NET and Python, with Go
and Dart libraries in the works. However, there is currently no native Scala
library and developing a wrapper around Google's Java library is unattractive
since the library is synchronous.

This project aims to provide a fully-asynchronous actor-based pure Scala library
for Gmail API.

## High-Level Project Goals
* Fully asynchronous
* Actor-based 
* Fast and lightweight
* Data model based on case classes and Scala collections
* Easily extensible to support additional Google and non-Google APIs
* Configuration via Typesafe Config
* Logging via Akka event bus
* Type safety

## Project Status

Project is under development. Code documentation is under way.

Pull requests, code reviews, comments and questions are all appreciated.

####Completed:
* OAuth, Labels, Messages, Threads, History, Attachments and Drafts APIs
* Test suites for all APIs

####TO DOs:
* Scala Docs
* Router and supervisor (including implementation of RetryPolicy)
* Performance comparison between synchronous Java vs. async Scala clients

####Future[TO DOs]:
* Akka-Http
* Use Google Discovery API and Scala reflection to create methods and resources
* Hide away nextPageToken from List methods for Threads, Messages and History
* Full inbox sync

Gmail API limits per user usage to 25 work units per second (moving average). For
more details, see <https://developers.google.com/gmail/api/v1/reference/quota>
Supervisors should implement exponential backoff when they receive TooManyRequests
from the actor. The library provides scaffolding for retry policy implementation 
(see gmailapi.restclient.RetryPolicy).

## Usage

```scala
import akka.actor._
import gmailapi.oauth2._
import gmailapi.methods._
import gmailapi.resources._
import gmailapi.restclient.RestResponses

// TO DO
...
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

Next, define REST service end-points as implementations of
`gmailapi.restclient.RestRequest` trait specifying concrete values for
abstract members ```uri```, ```method```, ```credentials```, ```entity``` and
```unmarshaller```:

```scala
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
  ...
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

Access the service as follows:
```
val myApi = system.actorOf(Props(new PeopleApiActor))
implicit val creds = OAuth2BearerToken("...")

val person = Person(
  givenName = "John",
  familyName = "Appleseed",
  email = Some("john.appleseed@icloud.com"))

myApi ! People.Create(person)
```

## Dependencies

* Akka (https://typesafe.com/platform/runtime/akka)
* Spray (http://spray.io)
* json4s (http://json4s.org)
* scalatest

## Installation

You can add the gmail-api-scala-client as a dependency as follows:

### SBT

```scala
    val gmailApiScalaClient = "com.github.nemanja-stanarevic" % "gmail-api-scala-client" % "0.1"
```

Made with ❤ in NYC at Hacker School <http://hackerschool.com>
