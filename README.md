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

Project is under heavy development. Test suite remains to be completed as well
as code documentation.

Pull requests, code reviews, comments and questions are all appreciated.

####Completed:
* OAuth API
* Labels API
* Messages API
* Threads API
* History API
* Attachments API
* Drafts API
* Test suites for OAuth and Labels APIs
* Test suite for Messages API
* Test suite for Threads API

####TO DOs:
* Test suite for History API
* Test suite for Drafts API
* Test suite for Attachments API
* Scala Docs
* Sample router and supervisor (including implementation of RetryPolicy)
* Define ListAll actor message for Gmail Messages, Threads and History
  - Actor sends all resources asynchronously in chunks of specified size
    rather than returning nextPageToken with each response and requiring the 
    client to make subsequent request

####Future[Maybes]:
* Support for Google's Discovery API using Scala reflection

Users should implement routers and supervisors appropriate for the specific
use cases. It will be important to consider Gmail API per user request
limitations (currently set at 25 work units per user/per second) and implement
exponential backoff and potentially per-user request throttling. The library
provides scaffolding for retry policy implementation (see gmailapi.restclient.RetryPolicy).

## Usage

```scala
import akka.actor._
import gmailapi.oauth2._
import gmailapi.methods._
import gmailapi.resources._
import gmailapi.restclient.RestResponses
...
```

TO DO

## Dependencies

* Akka (https://typesafe.com/platform/runtime/akka)
* Spray (http://spray.io)
* json4s (http://json4s.org)
* scalatest / scalacheck / specs2

## How it works

TO DO

## Installation

You can add the gmail-api-scala-client as a dependency as follows:

### SBT

```scala
    val gmailApiScalaClient = "com.github.nemanja-stanarevic" % "gmail-api-scala-client" % "0.1"
```

Made with ❤ in NYC at Hacker School <http://hackerschool.com>
