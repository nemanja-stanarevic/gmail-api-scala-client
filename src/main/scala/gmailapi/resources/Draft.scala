package gmailapi.resources

case class Draft (
  id: Option[String] = None,
  message: Option[Message] = None
  ) extends GmailResource {
  
  def updated(msg: Message) =
    Draft(id = this.id, message = Some(msg))    
}
