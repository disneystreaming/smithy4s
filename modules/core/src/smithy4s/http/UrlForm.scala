/*
 *  Copyright 2021-2023 Disney Streaming
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
package http

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import smithy4s.codecs.PayloadPath
import smithy4s.codecs.PayloadPath.Segment
import scala.collection.mutable
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.http.internals.UrlFormDataEncoder
import smithy4s.http.internals.UrlFormDataEncoderSchemaVisitor

import smithy4s.schema.Schema

final case class UrlForm(values: UrlForm.FormData.MultipleValues)

object UrlForm {

  sealed trait FormData extends Product with Serializable {
    def render: String
    def prepend(segment: PayloadPath.Segment): FormData
    def toPathedValues: Vector[FormData.PathedValue]
    def widen: FormData = this
  }
  object FormData {
    case object Empty extends FormData {
      override def render: String = ""

      override def prepend(segment: PayloadPath.Segment): FormData = this

      override def toPathedValues: Vector[FormData.PathedValue] = Vector.empty
    }

    // TODO: Rename as Value, replace uses by PathedValue?
    final case class SimpleValue(str: String) extends FormData {
      override def render: String =
        URLEncoder.encode(str, StandardCharsets.UTF_8.name())

      override def prepend(segment: PayloadPath.Segment): PathedValue =
        PathedValue(PayloadPath(segment), str)

      override def toPathedValues: Vector[FormData.PathedValue] = Vector.empty
    }
    // TODO: Make value an option?
    final case class PathedValue(path: PayloadPath, value: String)
        extends FormData {

      override def render: String = {
        val lastIndex = path.segments.size - 1
        val key = path.segments.zipWithIndex
          .foldLeft(new mutable.StringBuilder) {

            case (builder, (Segment.Label(label), i)) if i < lastIndex =>
              builder.append(
                URLEncoder.encode(label, StandardCharsets.UTF_8.name())
              )
              builder.append('.')

            case (builder, (Segment.Index(index), i)) if i < lastIndex =>
              builder.append(index)
              builder.append('.')

            case (builder, (Segment.Label(label), _)) =>
              builder.append(
                URLEncoder.encode(label, StandardCharsets.UTF_8.name())
              )

            case (builder, (Segment.Index(index), _)) =>
              builder.append(index)
          }
          .toString()

        key + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8.name())
      }

      override def prepend(segment: PayloadPath.Segment): PathedValue =
        copy(path.prepend(segment), value)

      override def toPathedValues: Vector[FormData.PathedValue] = Vector(this)
    }

    // TODO: Rename as Values?
    final case class MultipleValues(values: Vector[PathedValue])
        extends FormData {

      override def render: String =
        values.map(_.render).filter(str => str.nonEmpty).mkString("&")
      override def prepend(segment: PayloadPath.Segment): MultipleValues =
        copy(values.map(_.prepend(segment)))

      override def toPathedValues: Vector[FormData.PathedValue] = values
    }
  }

  trait Encoder[A] {
    def encode(a: A): UrlForm
  }
  object Encoder extends CachedSchemaCompiler.Impl[Encoder] {
    protected override type Aux[A] = UrlFormDataEncoder[A]
    def fromSchema[A](schema: Schema[A], cache: Cache): Encoder[A] = {
      val schemaVisitor = new UrlFormDataEncoderSchemaVisitor(cache)
      val urlFormDataEncoder = schemaVisitor(schema)
      (value: A) =>
        UrlForm(
          UrlForm.FormData.MultipleValues(
            urlFormDataEncoder.encode(value).toPathedValues
          )
        )
    }
  }

}
