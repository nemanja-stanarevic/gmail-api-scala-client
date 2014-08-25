package gmailapi.resources

case class Thread (
  id: String,
  snippet: String,
  historyId: Long,
  messages: Seq[Message]
) extends GmailResource 

