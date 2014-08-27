package gmailapi.resources

case class MessagePart(
  partId: Option[String],
  mimeType: Option[String],
  filename: Option[String] = None,
  headers: Seq[MessageHeader] = Nil,
  body: Option[MessageAttachment] = None,
  parts: Seq[MessagePart] = Nil) extends GmailResource 
