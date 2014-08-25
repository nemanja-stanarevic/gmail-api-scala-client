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
* Messages API (only Get, List, Delete)
* Threads API
* History API
* Test suites for OAuth, Labels APIs

TO DOs:
* Test suite for Messages API (List, Get)
* Test suite for Threads API
* Test suite for History API
* Complete Messages API (Insert, Modify, Send, Trash, Untrash, Import)
* Complete test suite for Message API (Delete, Insert, Modify, Send, Trash, Untrash, Import)
* Attachments API (Get)
* Test suite for Attachments API
* Drafts API
* Test suite for Drafts API
* Scala Docs
* Actor Routers and Supervisors
* ListAll methods for messages, threads and history (e.g. returns all resources in chunks rather than returning nextPageToken)

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