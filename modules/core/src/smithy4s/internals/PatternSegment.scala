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

package smithy4s.internals

import scala.collection.mutable.{ListBuffer, ArrayBuffer}

private[internals] sealed abstract class PatternSegment(value: String)
    extends Product
    with Serializable {
  def append(char: Char): PatternSegment
}

private[internals] object PatternSegment {
  final case class StaticSegment(val value: String)
      extends PatternSegment(value) {
    override def append(char: Char): PatternSegment =
      this.copy(value = this.value + char)
  }
  final case class ParameterSegment(
      val value: String,
      terminationChar: Option[Char]
  ) extends PatternSegment(value) {
    override def append(char: Char): PatternSegment =
      this.copy(value = this.value + char)
  }

  def segmentsFromString(str: String): List[PatternSegment] = {
    val segmentsSoFar = ListBuffer.empty[PatternSegment]
    val currentSegment = ArrayBuffer.empty[Char]
    var isInsideParameterSegment = false
    for ((nextChar, i) <- str.zipWithIndex) {
      if (isInsideParameterSegment) {
        if (nextChar == '}') {
          isInsideParameterSegment = false
          val terminationChar =
            if (i + 1 < str.length) Some(str.charAt(i + 1)) else None
          segmentsSoFar.append(
            PatternSegment.ParameterSegment(
              new String(currentSegment.toArray),
              terminationChar
            )
          )
          currentSegment.clear()

        } else {
          currentSegment.append(nextChar)
        }
      } else {
        if (nextChar == '{') {
          isInsideParameterSegment = true
          if (currentSegment.nonEmpty) {
            segmentsSoFar.append(
              PatternSegment.StaticSegment(new String(currentSegment.toArray))
            )
            currentSegment.clear()
          }
        } else {
          currentSegment.append(nextChar)
        }
      }
    }
    if (!isInsideParameterSegment && currentSegment.nonEmpty) {
      segmentsSoFar.append(
        PatternSegment.StaticSegment(new String(currentSegment.toArray))
      )
    }
    segmentsSoFar.toList
  }

}
