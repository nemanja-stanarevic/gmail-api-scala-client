package gmailapi.resources

case class History (
  id: Long,
  messages: Seq[Message]
) extends GmailResource 

