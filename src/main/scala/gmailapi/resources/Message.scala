package gmailapi.resources

case class Message (
  id: Option[String] = None,
  threadId: String,
  labelIds: Seq[String] = Nil,
  snippet: Option[String] = None,
  /* Gmail Messages API returns historyId as String, but it really shoule be Unsigned Long */
  historyId: Option[String] = None,
  payload: Option[MessagePart] = None,
  sizeEstimate: Option[Int] = None,
  raw: Option[String] = None
) extends GmailResource 

