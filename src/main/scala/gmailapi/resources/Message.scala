package gmailapi.resources

import java.io.ByteArrayOutputStream
import javax.mail.internet.{ InternetAddress, MimeMessage }
import org.apache.commons.mail._
import org.apache.commons.codec.binary.Base64
import javax.mail.Session

case class Message (
  id: Option[String] = None,
  threadId: Option[String] = None,
  labelIds: Seq[String] = Nil,
  snippet: Option[String] = None,
  /* Gmail Messages API returns historyId as String, but it really shoule be Unsigned Long */
  historyId: Option[String] = None,
  payload: Option[MessagePart] = None,
  sizeEstimate: Option[Int] = None,
  raw: Option[String] = None
) extends GmailResource 

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
	  htmlMsg: Option[String] = None) : Message = {
	  
	  var _message = new HtmlEmail()
	  _message.setHostName("smtp.gmail.com")

	  fromAddress foreach {case (name, email) => _message.setFrom(email, name)}
	  to foreach {case (name, email) => _message.addTo(email, name)}
	  cc foreach {case (name, email) => _message.addCc(email, name)}
	  bcc foreach {case (name, email) => _message.addBcc(email, name)}
	  subject foreach {_message.setSubject(_)}
	  textMsg foreach {msg => _message = _message.setTextMsg(msg)}
	  htmlMsg foreach {msg => _message = _message.setHtmlMsg(msg)}
	  
	  _message.buildMimeMessage()
	  val mimeMessage = _message.getMimeMessage()
	  
	  val baos : ByteArrayOutputStream = new ByteArrayOutputStream()
	  mimeMessage.writeTo(baos)
	  val encodedRaw = Base64.encodeBase64URLSafeString(baos.toByteArray)
	  
	  Message(threadId = threadId, labelIds = labelIds, raw = Some(encodedRaw))
	}
}