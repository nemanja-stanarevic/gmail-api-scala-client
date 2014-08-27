package gmailapi.resources

case class MessageAttachment(
  attachmentId: Option[String],
  size: Option[Int],
  data: Option[String]) extends GmailResource 
