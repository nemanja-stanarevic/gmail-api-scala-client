package gmailapi

import com.typesafe.config.Config
import spray.http.Uri

package object oauth2 {
  
  def authorizationUri(loginHint: Option[String] = None)(implicit config: Config) =
    Uri("https://accounts.google.com/o/oauth2/auth") withQuery (
      "response_type" -> "code",
      "client_id" -> config.getString("oauth2.clientId"),
      "redirect_uri" -> config.getString("oauth2.redirectUri"),
      "scope" -> config.getString("oauth2.scope"),
      "access_type" -> "offline",
      "approval_prompt" -> "force",
      "login_hint" -> 
        (loginHint match {
          case Some(x) => x
          case None => ""
        })
    ) toString
          
}