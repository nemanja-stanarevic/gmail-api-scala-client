package gmailapi.resources

case class DraftList(
  drafts: Seq[Draft] = Nil,
  nextPageToken: Option[String] = None,
  resultSizeEstimate: Option[Int] = None) extends GmailResource 
