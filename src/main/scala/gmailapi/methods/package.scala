package gmailapi

import gmailapi.restclient.RestRequest
import gmailapi.oauth2.OAuth2Identity
import spray.http.{ HttpCredentials, OAuth2BearerToken }

package object methods {

  trait GmailRestRequest extends RestRequest {
    val baseUri = "https://www.googleapis.com/gmail/v1"
  }

  implicit def oauth2IdToBearerToken(id: OAuth2Identity): Option[HttpCredentials] =
    Some(OAuth2BearerToken(id.accessToken))
}
