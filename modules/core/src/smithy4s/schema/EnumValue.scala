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
package schema

case class EnumValue[E] private (
    stringValue: String,
    intValue: Int,
    value: E,
    name: String,
    hints: Hints
) {
  def withStringValue(value: String): EnumValue[E] = {
    copy(stringValue = value)
  }

  def withIntValue(value: Int): EnumValue[E] = {
    copy(intValue = value)
  }

  def withValue(value: E): EnumValue[E] = {
    copy(value = value)
  }

  def withName(value: String): EnumValue[E] = {
    copy(name = value)
  }

  def withHints(value: Hints): EnumValue[E] = {
    copy(hints = value)
  }
  def map[A](f: E => A): EnumValue[A] =
    copy(value = f(value))

  def transformHints(f: Hints => Hints): EnumValue[E] =
    copy(hints = f(hints))
}

object EnumValue {
  @scala.annotation.nowarn(
    "msg=private method unapply in object EnumValue is never used"
  )
  private def unapply[E](c: EnumValue[E]): Option[EnumValue[E]] = Some(c)
  def apply[E](
      stringValue: String,
      intValue: Int,
      value: E,
      name: String,
      hints: Hints
  ): EnumValue[E] = {
    new EnumValue(stringValue, intValue, value, name, hints)
  }

}
