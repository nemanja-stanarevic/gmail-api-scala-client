package gmailapi.resources

case class History(
  id: String,
  messages: Seq[Message]) extends GmailResource 
