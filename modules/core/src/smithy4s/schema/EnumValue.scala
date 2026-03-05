/*
 *  Copyright 2021-2026 Disney Streaming
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

final class EnumValue[E](
    val stringValue: String,
    val intValue: Int,
    val value: E,
    val name: String,
    val hints: Hints
) {
  def map[A](f: E => A): EnumValue[A] =
    new EnumValue(stringValue, intValue, f(value), name, hints)

  def transformHints(f: Hints => Hints): EnumValue[E] =
    new EnumValue(stringValue, intValue, value, name, f(hints))

  override def equals(obj: Any): Boolean = obj match {
    case that: EnumValue[_] => this.stringValue == that.stringValue && this.intValue == that.intValue && this.value == that.value && this.name == that.name && this.hints == that.hints
    case _ => false
  }
  override def hashCode(): Int = {
    var result = stringValue.##
    result = 31 * result + intValue.##
    result = 31 * result + value.##
    result = 31 * result + name.##
    result = 31 * result + hints.##
    result
  }
  override def toString: String = s"EnumValue($stringValue, $intValue, $value, $name, $hints)"
}
object EnumValue {
  def apply[E](stringValue: String, intValue: Int, value: E, name: String, hints: Hints): EnumValue[E] =
    new EnumValue(stringValue, intValue, value, name, hints)
  def unapply[E](x: EnumValue[E]): Some[(String, Int, E, String, Hints)] =
    Some((x.stringValue, x.intValue, x.value, x.name, x.hints))
}
