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

package smithy4s
package aws.query

import smithy4s.PayloadPath
import smithy4s.PayloadPath.Segment
import scala.collection.mutable
import smithy4s.http.internals.URIEncoderDecoder

private[aws] sealed trait FormData extends Product with Serializable {
  def render: String
  def prepend(key: String): FormData
  def prepend(index: Int): FormData
}
private[aws] object FormData {
  case object Empty extends FormData {
    override def render: String = ""

    override def prepend(key: String): FormData = this

    override def prepend(index: Int): FormData = this
  }

  final case class SimpleValue(str: String) extends FormData {
    override def render: String = URIEncoderDecoder.encode(str)

    override def prepend(key: String): FormData = {
      PathedValue(PayloadPath(key), str)
    }

    override def prepend(index: Int): FormData =
      PathedValue(PayloadPath(index), str)
  }
  final case class PathedValue(path: PayloadPath, value: String)
      extends FormData {

    /**
     * @todo Understand the root reason and have a better solution for a workaround removing the '.' prefix.
     */
    override def render: String = {
      val lastIndex = path.segments.size - 1
      val key = path.segments.zipWithIndex
        .foldLeft(new mutable.StringBuilder) {

          case (builder, (Segment.Label(label), i)) if i < lastIndex =>
            builder.append(URIEncoderDecoder.encode(label))
            builder.append('.')

          case (builder, (Segment.Index(index), i)) if i < lastIndex =>
            builder.append(index)
            builder.append('.')

          case (builder, (Segment.Label(label), _)) =>
            builder.append(URIEncoderDecoder.encode(label))

          case (builder, (Segment.Index(index), _)) =>
            builder.append(index)
        }
        .toString()

      key + "=" + URIEncoderDecoder.encode(value)
    }

    override def prepend(key: String): FormData =
      copy(path.prepend(PayloadPath.Segment(key)), value)

    override def prepend(index: Int): FormData =
      copy(path.prepend(PayloadPath.Segment(index)), value)
  }
  final case class MultipleValues(values: Vector[FormData]) extends FormData {
    override def render: String =
      values.map(_.render).filter(str => str.nonEmpty).mkString("&")

    override def prepend(key: String): FormData =
      copy(values.map(_.prepend(key)))

    override def prepend(index: Int): FormData =
      copy(values.map(_.prepend(index)))
  }
}
