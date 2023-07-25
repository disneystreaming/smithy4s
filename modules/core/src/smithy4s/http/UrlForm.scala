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

private[smithy4s] final case class UrlForm(
    formData: UrlForm.FormData.MultipleValues
)

private[smithy4s] object UrlForm {

  sealed trait FormData extends Product with Serializable {
    def prepend(segment: PayloadPath.Segment): FormData
    def render: String = {
      val builder = new mutable.StringBuilder
      writeTo(builder)
      builder.result()
    }
    def toPathedValues: Vector[FormData.PathedValue]
    def widen: FormData = this
    def writeTo(builder: mutable.StringBuilder): Unit
  }
  object FormData {
    case object Empty extends FormData {

      override def prepend(segment: PayloadPath.Segment): FormData = this

      override def toPathedValues: Vector[FormData.PathedValue] = Vector.empty

      override def writeTo(builder: mutable.StringBuilder): Unit = ()

    }

    // TODO: Rename as Value, replace uses by PathedValue?
    final case class SimpleValue(string: String) extends FormData {

      override def prepend(segment: PayloadPath.Segment): PathedValue =
        PathedValue(PayloadPath(segment), maybeValue = Some(string))

      override def toPathedValues: Vector[FormData.PathedValue] = Vector.empty

      override def writeTo(builder: mutable.StringBuilder): Unit =
        builder.append(
          URLEncoder.encode(string, StandardCharsets.UTF_8.name())
        )

    }

    object PathedValue {
      def apply(path: PayloadPath, value: String): PathedValue =
        PathedValue(path, maybeValue = Some(value))
    }

    final case class PathedValue(path: PayloadPath, maybeValue: Option[String])
        extends FormData {

      override def prepend(segment: PayloadPath.Segment): PathedValue =
        copy(path.prepend(segment), maybeValue)

      override def toPathedValues: Vector[FormData.PathedValue] = Vector(this)

      override def writeTo(builder: mutable.StringBuilder): Unit = {
        val lastIndex = path.segments.size - 1
        var i = 0
        for (segment <- path.segments) {
          builder.append(segment match {
            case Segment.Label(label) =>
              URLEncoder.encode(label, StandardCharsets.UTF_8.name())

            case Segment.Index(index) =>
              index
          })
          if (i < lastIndex) builder.append('.')
          i += 1
        }
        builder.append('=')
        maybeValue.foreach(value =>
          builder.append(
            URLEncoder.encode(value, StandardCharsets.UTF_8.name())
          )
        )
      }
    }

    // TODO: Rename as Values?
    final case class MultipleValues(values: Vector[PathedValue])
        extends FormData {

      override def prepend(segment: PayloadPath.Segment): MultipleValues =
        copy(values.map(_.prepend(segment)))

      override def toPathedValues: Vector[FormData.PathedValue] = values

      override def writeTo(builder: mutable.StringBuilder): Unit = {
        val lastIndex = values.size - 1
        var i = 0
        for (value <- values) {
          value.writeTo(builder)
          if (i < lastIndex) builder.append('&')
          i += 1
        }
      }
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
