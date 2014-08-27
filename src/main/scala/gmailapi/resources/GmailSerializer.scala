package gmailapi.resources

import spray.httpx.Json4sJacksonSupport
import org.json4s.{ DefaultFormats, FieldSerializer }

object GmailSerializer extends Json4sJacksonSupport {
  implicit def json4sJacksonFormats = DefaultFormats +
    new org.json4s.ext.EnumNameSerializer(LabelListVisibility) +
    new org.json4s.ext.EnumNameSerializer(LabelOwnerType) +
    new org.json4s.ext.EnumNameSerializer(MessageListVisibility) +
    FieldSerializer[Label]() +
    FieldSerializer[MessageAttachment]() +
    FieldSerializer[MessageHeader]() +
    FieldSerializer[MessagePart]() +
    FieldSerializer[Message]() +
    FieldSerializer[MessageList]() +
    FieldSerializer[Thread]() +
    FieldSerializer[ThreadList]() +
    FieldSerializer[History]() +
    FieldSerializer[HistoryList]() +
    FieldSerializer[Draft]() +
    FieldSerializer[DraftList]()
}
