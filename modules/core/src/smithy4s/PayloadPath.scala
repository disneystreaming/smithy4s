/*
 *  Copyright 2021 Disney Streaming
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

case class PayloadPath(segments: List[PayloadPath.Segment]) {
  override def toString = PayloadPath.asString(this)
}

object PayloadPath {

  val root = PayloadPath(List.empty)

  def apply(segments: PayloadPath.Segment*): PayloadPath = PayloadPath(
    segments.toList
  )

  def fromString(str: String): PayloadPath = PayloadPath(
    str.split('.').filter(_.nonEmpty).map(Segment.fromString).toList
  )
  def asString(path: PayloadPath): String = path.segments
    .map {
      case Segment.Label(str) => str
      case Segment.Index(idx) => idx.toString
    }
    .mkString(".", ".", "")

  val schema: Schema[PayloadPath] =
    Schema.bijection(Schema.string, fromString, asString)

  /**
    * A path-segment in a json-like object
    */
  sealed trait Segment

  object Segment {
    def apply(label: String): Segment = Label(label)
    def apply(index: Int): Segment = Index(index)

    def fromString(str: String): Segment = try { Index(str.toInt) }
    catch {
      case _: Throwable => Label(str)
    }

    case class Label(label: String) extends Segment
    case class Index(index: Int) extends Segment

    implicit def stringConversion(label: String): Segment = Label(label)
    implicit def intConversion(index: Int): Segment = Index(index)
  }

}
