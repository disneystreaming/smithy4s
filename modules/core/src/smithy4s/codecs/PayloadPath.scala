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

package smithy4s.codecs

import smithy4s.schema._

case class PayloadPath(segments: List[PayloadPath.Segment]) {

  def append(segment: PayloadPath.Segment): PayloadPath =
    copy(segments ::: List(segment))

  def prepend(segment: PayloadPath.Segment): PayloadPath =
    copy(segment :: segments)

  override def toString = render()

  def render(prefix: String = "."): String =
    segments.map(_.render).mkString(prefix, ".", "")

}

object PayloadPath {

  val root = PayloadPath(segments = List.empty)

  def apply(segments: PayloadPath.Segment*): PayloadPath = PayloadPath(
    segments.toList
  )

  def parse(string: String): PayloadPath = PayloadPath(
    string.split('.').filter(_.nonEmpty).map(Segment.parse).toList
  )

  val schema: Schema[PayloadPath] =
    Schema.bijection(Schema.string, parse, _.render())

  /**
    * A path-segment in a json-like object
    */
  sealed trait Segment {
    def render: String
  }

  object Segment {
    def apply(label: String): Segment = Label(label)
    def apply(index: Int): Segment = Index(index)

    // Yes, this smells a bit, because there is no type information in the
    // rendered form - i.e. no way to know if a string that parses as an int is
    // definitely meant to be read that way vs. read as just a string. Callers
    // of this function beware.
    def parse(string: String): Segment = try { Index(string.toInt) }
    catch {
      case _: Throwable => Label(string)
    }

    case class Label(label: String) extends Segment {
      override lazy val render: String = label
    }

    object Label {
      def apply(label: String): Label = {
        new Label(label)
      }
    }
    case class Index(index: Int) extends Segment {
      override lazy val render: String = index.toString
    }

    object Index {
      def apply(index: Int): Index = {
        new Index(index)
      }
    }

    implicit def stringConversion(label: String): Segment = Label(label)
    implicit def intConversion(index: Int): Segment = Index(index)
  }

}
