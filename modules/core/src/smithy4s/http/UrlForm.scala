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

import smithy4s.codecs.PayloadPath
import smithy4s.codecs.PayloadPath.Segment
import smithy4s.http.internals.UrlFormCursor
import smithy4s.http.internals.UrlFormDataDecoder
import smithy4s.http.internals.UrlFormDataDecoderSchemaVisitor
import smithy4s.http.internals.UrlFormDataEncoder
import smithy4s.http.internals.UrlFormDataEncoderSchemaVisitor
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.schema.Schema

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import scala.collection.mutable

final case class UrlForm(
    values: List[UrlForm.FormData]
) {

  def render: String = {
    val builder = new mutable.StringBuilder
    writeTo(builder)
    builder.result()
  }

  def writeTo(builder: mutable.StringBuilder): Unit = {
    val lastIndex = values.size - 1
    var i = 0
    for (value <- values) {
      value.writeTo(builder)
      if (i < lastIndex) builder.append('&')
      i += 1
    }
  }
}

private[smithy4s] object UrlForm {

  final case class FormData(path: PayloadPath, maybeValue: Option[String]) {

    def prepend(segment: PayloadPath.Segment): FormData =
      copy(path.prepend(segment), maybeValue)

    def writeTo(builder: mutable.StringBuilder): Unit = {
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
      if (i > 0) builder.append('=')
      maybeValue.foreach(value =>
        builder.append(
          URLEncoder.encode(value, StandardCharsets.UTF_8.name())
        )
      )
    }
  }

  trait Decoder[A] {
    def decode(urlForm: UrlForm): Either[UrlFormDecodeError, A]
  }

  object Decoder {
    def apply(
        ignoreXmlFlattened: Boolean,
        capitalizeStructAndUnionMemberNames: Boolean
    ): CachedSchemaCompiler[Decoder] =
      new CachedSchemaCompiler.Impl[Decoder] {
        protected override type Aux[A] = UrlFormDataDecoder[A]
        override def fromSchema[A](
            schema: Schema[A],
            cache: Cache
        ): Decoder[A] = {
          val schemaVisitor =
            new UrlFormDataDecoderSchemaVisitor(
              cache,
              ignoreXmlFlattened,
              capitalizeStructAndUnionMemberNames
            )
          val urlFormDataDecoder = schemaVisitor(schema)
          urlForm =>
            urlFormDataDecoder.decode(
              UrlFormCursor.fromUrlForm(urlForm)
            )
        }
      }
  }

  trait Encoder[A] {
    def encode(a: A): UrlForm
  }

  object Encoder {
    def apply(
        ignoreXmlFlattened: Boolean,
        capitalizeStructAndUnionMemberNames: Boolean
    ): CachedSchemaCompiler[Encoder] =
      new CachedSchemaCompiler.Impl[Encoder] {
        protected override type Aux[A] = UrlFormDataEncoder[A]
        override def fromSchema[A](
            schema: Schema[A],
            cache: Cache
        ): Encoder[A] = {
          val schemaVisitor =
            new UrlFormDataEncoderSchemaVisitor(
              cache,
              ignoreXmlFlattened,
              capitalizeStructAndUnionMemberNames
            )
          val urlFormDataEncoder = schemaVisitor(schema)
          value => UrlForm(urlFormDataEncoder.encode(value))
        }
      }
  }
}
