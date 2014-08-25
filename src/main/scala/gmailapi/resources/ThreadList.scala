package gmailapi.resources

case class ThreadList (
  messages: Seq[Thread] = Nil,
  nextPageToken: Option[String] = None,
  resultSizeEstimate: Option[Int] = None
) extends GmailResource 

