# gmail-api-scala-client

Google published Gmail API libraries for Java, .NET and Python, with Go and Dart in the works. 
However, there is currently no native Scala library and existing libraries are synchronous.

This project aims to provide a fully-asynchronous actor-based pure Scala Gmail API library. 

High level goals:
* Asynchronous
* Actor-based 
* Fast and lightweight
* Case class and Scala collections-based data model
* Easily extensible to support additional Google and non-Google APIs
* Unified configuration via Typesafe config
* Unified logging via Akka event bus
* Type safety

## Usage


```scala
import akka.actor._
import gmailapi.oauth2._
import gmailapi.methods._
import gmailapi.resources._
import gmailapi.restclient.RestResponses
...
```
TO DO...

Dependencies
============
* Akka (https://typesafe.com/platform/runtime/akka)
* Spray (http://spray.io)
* json4s (http://json4s.org)
* scalatest / scalacheck / specs2

Status
======

Project is under development and significant functionality remains to be implemented. 

COMPLETED:
* OAuth API
* Labels API
* Tests for OAuth, Labels APIs

TO DOs:
* Messages / Attachments API
* Threads API
* History API
* Drafts API
* Tests for all of the above APIs
* Scala Docs

Installation
============

You can add the gmail-api-scala-client as a dependency in following ways.

### SBT users

    val gmailApiScalaClient = "com.github.nemanja-stanarevic" % "gmail-api-scala-client" % "0.1"
