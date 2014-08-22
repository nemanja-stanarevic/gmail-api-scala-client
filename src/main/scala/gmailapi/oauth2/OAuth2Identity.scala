package gmailapi.oauth2

import gmailapi.resources.GmailResource
import org.json4s.{DefaultFormats, FieldSerializer}
import spray.httpx.Json4sJacksonSupport

case class OAuth2Identity(
  accessToken: String,
  refreshToken: String,
  expiration: Long,
  userId: Option[String] = None, 
  email: Option[String] = None, 
  scope: Seq[String] = Nil,
  name: Option[String] = None,
  givenName: Option[String] = None,
  familyName: Option[String] = None,
  picture: Option[String] = None,
  gender: Option[String] = None,
  locale: Option[String] = None) extends GmailResource
 