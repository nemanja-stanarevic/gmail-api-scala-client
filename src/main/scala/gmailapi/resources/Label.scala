package gmailapi.resources

object MessageListVisibility extends Enumeration {
    val Hide = Value("hide")
    val Show = Value("show")
}

object LabelListVisibility extends Enumeration {
    val LabelHide = Value("labelHide")
    val LabelShow = Value("labelShow")
    val LabelShowIfUnread = Value("labelShowIfUnread")
}

object LabelOwnerType extends Enumeration {
    val System = Value("system")
    val User = Value("user")
}

import MessageListVisibility._
import LabelListVisibility._
import LabelOwnerType._

case class Label (
  id : Option[String] = None,
  name : String,
  messageListVisibility: MessageListVisibility.Value = Show,
  labelListVisibility: LabelListVisibility.Value = LabelShow,
  ownerType: Option[LabelOwnerType.Value] = None
) extends GmailResource 

