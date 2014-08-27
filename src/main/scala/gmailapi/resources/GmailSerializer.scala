/*
 * Copyright © 2014 Nemanja Stanarevic <nemanja@alum.mit.edu>
 *
 * Made with ❤ in NYC at Hacker School <http://hackerschool.com>
 *
 * Licensed under the GNU Affero General Public License, Version 3
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 *
 * <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gmailapi.resources

import spray.httpx.Json4sJacksonSupport
import org.json4s.{ DefaultFormats, FieldSerializer, Formats }

object GmailSerializer extends Json4sJacksonSupport {
  implicit def json4sJacksonFormats : Formats = DefaultFormats +
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
