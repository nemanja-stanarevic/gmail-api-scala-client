# gmail-api-scala-client

# Code Climate

[![Build Status](https://travis-ci.org/nemanja-stanarevic/gmail-api-scala-client.svg?branch=master)](https://travis-ci.org/nemanja-stanarevic/gmail-api-scala-client)

# Motivation
Google published Gmail API libraries for Java, .NET and Python, with Go and Dart in the works. 
However, there is currently no native Scala library and developing a wrapper around Google's Java library 
is unattractive since the library is synchronous.

This project aims to provide a fully-asynchronous actor-based pure Scala library for Gmail API.

# High level project goals
* Fully asynchronous
* Actor-based 
* Fast and lightweight
* Data model based on case classes and Scala collections
* Easily extensible to support additional Google and non-Google APIs
* Configuration via Typesafe Config
* Logging via Akka event bus
* Type safety

Project Status
==============

Project is under heavy development and significant functionality remains to be implemented. 

COMPLETED:
* OAuth API
* Labels API
* Test suites for OAuth, Labels APIs
* Messages API
* Test suite for Messages API (Insert, List, Get, Delete)
* Threads API
* History API
* Attachments API
* Drafts API (Create, Delete, Get, List, Update, Send)

TO DOs:
* Complete test suite for Message API (Modify, Send, Trash, Untrash, Import)
* Test suite for Threads API
* Test suite for History API
* Test suite for Attachments API
* Test suite for Drafts API
* Scala Docs
* Routers and Supervisors
* Gmail per-user request throttling
* New ListAll methods for messages, threads and history 
  - Method returns all resources asynchronously in chunks of specified size rather than returning nextPageToken
    with each response and having to make another request

MAYBEs:
* Support for Google's Discovery API using Scala reflection

Usage
=====

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

Installation
============

You can add the gmail-api-scala-client as a dependency as follows:

### SBT

```scala
    val gmailApiScalaClient = "com.github.nemanja-stanarevic" % "gmail-api-scala-client" % "0.1"
```