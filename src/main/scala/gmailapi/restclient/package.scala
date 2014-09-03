package gmailapi

/** Provides classes for creating REST clients.
  *
  * ==Overview==
  * The package includes traits and case classes for constructing REST service
  * clients.
  *
  * First, define resources that are produced/consumed by the REST service, as
  * follows:
  * {{{
  * case class Person(
  *   userId: Option[String] = None,
  *   givenName: String,
  *   familyName: String,
  *   email: Option[String] = None,
  *   pictureUri: Option[String] = None)
  * }}}
  *
  * Next, define serializers and deserializers for the resources. If REST
  * service is based on JSON format this can be done easily using spray and
  * json4s, as follows:
  * {{{
  * import spray.httpx.Json4sJacksonSupport
  * import org.json4s.{ DefaultFormats, FieldSerializer, Formats }
  *
  * object PersonSerializer extends Json4sJacksonSupport {
  *   implicit def json4sJacksonFormats : Formats = DefaultFormats +
  *   FieldSerializer[Person]() }
  * }}}
  *
  * Next, define REST service end-points as implementations of 
  * [[gmailapi.restclient.RestRequest]] trait specifying concrete values for 
  * abstract members:
  * {{{
  * import org.json4s.jackson.Serialization.{ read, write }
  * import spray.http.{HttpCredentials, HttpEntity, HttpMethods, ContentTypes}
  *
  * object People {
  *   import PersonSerializer._
  *
  *   case class Create(person: Person)(implicit val token: HttpCredentials)
  *     extends RestRequest {
  *
  *     val uri = s"https://api.myservice.com/people"
  *     val method = HttpMethods.POST
  *     val credentials = Some(token)
  *     val entity = HttpEntity(ContentTypes.`application/json`, write(person))
  *     val unmarshaller = Some(read[Person](_: String))
  *
  *   case class Get(userId: String)(implicit val token: HttpCredentials)
  *     extends RestRequest {
  *
  *     val uri = s"https://api.myservice.com/people/$userId"
  *     val method = HttpMethods.GET
  *     val credentials = Some(token)
  *     val entity = HttpEntity.Empty
  *     val unmarshaller = Some(read[Person](_: String))
  *
  *   ...
  * }
  * }}}
  * 
  * Finally, define a concrete [[akka.actor.Actor]] for the REST service by 
  * implementing RestActor trait.
  * {{{
  * class PeopleApiActor extends Actor with RestActor {
  *   // define a logger
  *   val log = Logging(context.system, this)
  *
  *   // define error handlers
  *   val errorHandler = List(
  *     RestActor.ErrorHandler(
  *       // when http response is...
  *       StatusCodes.Unauthorized,
  *       // reply with...
  *       RestResponses.ExpiredAuthToken) {
  *         // when this condition is satisfied
  *         _.contains("Expired Authentication Token")
  *       },
  *       // other error handlers...
  *     )
  * }
  * }}}
  * 
  * Access the service as follows:
  * {{{
  * val myApi = system.actorOf(Props(new PeopleApiActor))
  * implicit val creds = OAuth2BearerToken("...")
  *
  * val person = Person(
  *   givenName = "John",
  *   familyName = "Appleseed",
  *   email = Some("john.appleseed@icloud.com"))
  *
  * myApi ! People.Create(person)
  * }}}
  */
package object restclient {
}
