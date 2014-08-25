package gmailapi.resources

case class MessageList (
  messages: Seq[Message] = Nil,
  nextPageToken: Option[String] = None,
  resultSizeEstimate: Option[Int] = None
) extends GmailResource 

