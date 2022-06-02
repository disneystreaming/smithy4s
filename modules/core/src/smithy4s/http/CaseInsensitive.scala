/*
 *  Copyright 2021-2022 Disney Streaming
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

package smithy4s.http

final class CaseInsensitive private (override val toString: String)
    extends scala.math.Ordered[CaseInsensitive]
    with Serializable {
  override def equals(that: Any): Boolean =
    that match {
      case that: CaseInsensitive =>
        this.toString.equalsIgnoreCase(that.toString)
      case _ => false
    }

  @transient private[this] var hash = 0
  override def hashCode(): Int = {
    if (hash == 0)
      hash = calculateHash
    hash
  }

  private[this] def calculateHash: Int = {
    var h = 17
    var i = 0
    val len = toString.length
    while (i < len) {
      // Strings are equal igoring case if either their uppercase or lowercase
      // forms are equal. Equality of one does not imply the other, so we need
      // to go in both directions. A character is not guaranteed to make this
      // round trip, but it doesn't matter as long as all equal characters
      // hash the same.
      h = h * 31 + toString.charAt(i).toUpper.toLower
      i += 1
    }
    h
  }

  override def compare(that: CaseInsensitive): Int =
    this.toString.compareToIgnoreCase(that.toString)

  def isEmpty: Boolean = this.toString.isEmpty

  def nonEmpty: Boolean = this.toString.nonEmpty

  def trim: CaseInsensitive = CaseInsensitive(toString.trim)

  def startsWith(other: CaseInsensitive): Boolean = {
    if (other.length > this.length) false
    else CaseInsensitive(toString.substring(0, other.length)) == other
  }
  def startsWith(str: String): Boolean =
    startsWith(CaseInsensitive(str))

  def length: Int = toString.length

  def value: String = toString
}

object CaseInsensitive {
  def apply(value: String): CaseInsensitive = new CaseInsensitive(value)
  val empty = CaseInsensitive("")
}
