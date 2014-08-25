package gmailapi.resources

case class HistoryList (
  history: Seq[History] = Nil,
  nextPageToken: Option[String] = None,
  historyId: Long
) extends GmailResource 

