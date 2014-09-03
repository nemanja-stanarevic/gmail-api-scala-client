/*
 * Copyright © 2014 Nemanja Stanarevic <nemanja@alum.mit.edu>
 *
 * Made with ❤ in NYC at Hacker School <http://hackerschool.com>
 *
 * Licensed under the GNU Affero General Public License, Version 3
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 *
 * <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gmailapi.resources

import java.io.ByteArrayOutputStream
import javax.mail.internet.{ InternetAddress, MimeMessage }
import org.apache.commons.mail._
import org.apache.commons.codec.binary.Base64
import javax.mail.Session

case class Message(
  id: Option[String] = None,
  threadId: Option[String] = None,
  labelIds: Seq[String] = Nil,
  snippet: Option[String] = None,
  // Gmail Messages API returns historyId as String, but it really
  // should be Unsigned Long
  historyId: Option[String] = None,
  payload: Option[MessagePart] = None,
  sizeEstimate: Option[Int] = None,
  raw: Option[String] = None) extends GmailResource

object MessageFormat extends Enumeration {
  val Full = Value("full")
  val Minimal = Value("minimal")
  val Raw = Value("raw")
}

object MessageFactory {
  def createMessage(
    threadId: Option[String] = None,
    labelIds: Seq[String] = Nil,
    fromAddress: Option[(String, String)] = None,
    to: Seq[(String, String)] = Nil,
    cc: Seq[(String, String)] = Nil,
    bcc: Seq[(String, String)] = Nil,
    subject: Option[String] = None,
    textMsg: Option[String] = None,
    htmlMsg: Option[String] = None,
    attachments: Seq[(String, String)] = Nil): Message = {

    var _message = new HtmlEmail()
    _message.setHostName("smtp.gmail.com")

    fromAddress foreach { case (name, email) => _message.setFrom(email, name) }
    to foreach { case (name, email) => _message.addTo(email, name) }
    cc foreach { case (name, email) => _message.addCc(email, name) }
    bcc foreach { case (name, email) => _message.addBcc(email, name) }
    subject foreach { _message.setSubject(_) }
    textMsg foreach { msg => _message = _message.setTextMsg(msg) }
    htmlMsg foreach { msg => _message = _message.setHtmlMsg(msg) }
    attachments foreach {
      case (url, name) =>
        _message.attach(new java.net.URL(url), name, "")
    }
    _message.buildMimeMessage()
    val mimeMessage = _message.getMimeMessage()

    val baos: ByteArrayOutputStream = new ByteArrayOutputStream()
    mimeMessage.writeTo(baos)
    val encodedRaw = Base64.encodeBase64URLSafeString(baos.toByteArray)

    Message(threadId = threadId, labelIds = labelIds, raw = Some(encodedRaw))
  }
}
