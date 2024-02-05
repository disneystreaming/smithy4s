/*
 *  Copyright 2021-2024 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package smithy4s

final case class ConstraintError private (hint: Hint, message: String)
    extends Throwable
    with scala.util.control.NoStackTrace {
  def withHint(value: Hint): ConstraintError = {
    copy(hint = value)
  }

  def withMessage(value: String): ConstraintError = {
    copy(message = value)
  }
  override def getMessage() = s"$hint: $message"
}

object ConstraintError {
  @scala.annotation.nowarn(
    "msg=private method unapply in object ConstraintError is never used"
  )
  private def unapply(c: ConstraintError): Option[ConstraintError] = Some(c)
  def apply(hint: Hint, message: String): ConstraintError = {
    new ConstraintError(hint, message)
  }
}
