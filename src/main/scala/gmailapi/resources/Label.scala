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

case class Label(
  id: Option[String] = None,
  name: String,
  messageListVisibility: MessageListVisibility.Value = Show,
  labelListVisibility: LabelListVisibility.Value = LabelShow,
  ownerType: Option[LabelOwnerType.Value] = None) extends GmailResource
