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
import smithy4s.codecs._

import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import scala.collection.immutable.BitSet
import scala.collection.mutable
import smithy4s.kinds.PolyFunction

private[smithy4s] final case class UrlForm(values: List[UrlForm.FormData]) {

  def render: String = {
    val builder = new mutable.StringBuilder
    val lastIndex = values.size - 1
    var i = 0
    for (value <- values) {
      value.writeTo(builder)
      if (i < lastIndex) builder.append('&')
      i += 1
    }
    builder.result()
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
      builder.append('=')
      maybeValue.foreach(value =>
        builder.append(
          URLEncoder.encode(value, StandardCharsets.UTF_8.name())
        )
      )
    }
  }

  // This is based on http4s' own equivalent, but simplified for our use case.
  def parse(urlFormString: String): Either[UrlFormDecodeError, UrlForm] = {
    val inputBuffer = CharBuffer.wrap(urlFormString)
    val encodedTermBuilder = new StringBuilder(capacity = 32)
    val outputBuilder = List.newBuilder[UrlForm.FormData]

    var state: State = Key
    var error: UrlFormDecodeError = null
    var key: String = null

    def endPair(): Unit = {
      appendPair()
      state = Key
    }

    def appendPair(): Unit = if (state == Key) {
      outputBuilder += UrlForm.FormData(
        PayloadPath.parse(decodeTerm(encodedTermBuilder.result())),
        maybeValue = None
      )
      encodedTermBuilder.clear()
    } else {
      outputBuilder += UrlForm.FormData(
        PayloadPath.parse(decodeTerm(key)),
        Some(decodeTerm(encodedTermBuilder.result()))
      )
      key = null
      encodedTermBuilder.clear()
    }

    def decodeTerm(str: String): String =
      try URLDecoder.decode(str, StandardCharsets.UTF_8.name())
      catch {
        case _: UnsupportedEncodingException => ""
      }

    while (error == null && inputBuffer.hasRemaining)
      inputBuffer.get() match {
        case '&' => endPair()
        case '=' =>
          if (state == Value) encodedTermBuilder.append('=')
          else {
            state = Value
            key = encodedTermBuilder.result()
            encodedTermBuilder.clear()
          }
        case char if QueryChars.contains(char.toInt) =>
          encodedTermBuilder.append(char)
        case char =>
          error = UrlFormDecodeError(
            PayloadPath.root,
            s"Invalid char while splitting key/value pairs: '$char'"
          )
      }

    if (error != null) Left(error)
    else {
      appendPair()
      Right(UrlForm(outputBuilder.result()))
    }
  }

  private sealed trait State
  private case object Key extends State
  private case object Value extends State

  // These are the characters that are allowed unquoted within a query string as
  // defined in https://datatracker.ietf.org/doc/html/rfc3986#appendix-A.
  private val QueryChars: BitSet = BitSet(
    (Pchar ++ "/?".toSet - '&' - '=').map(_.toInt).toSeq: _*
  )

  private def Pchar = Unreserved ++ SubDelims ++ ":@%".toSet
  private def Unreserved = "-._~".toSet ++ AlphaNum
  private def AlphaNum = (('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')).toSet
  private def SubDelims = "!$&'()*+,;=".toSet

  private[smithy4s] trait Decoder[A] {
    def decode(urlForm: UrlForm): Either[UrlFormDecodeError, A]
  }

  object Decoder {
    def apply(
        ignoreUrlFormFlattened: Boolean,
        capitalizeStructAndUnionMemberNames: Boolean
    ): CachedSchemaCompiler[Decoder] =
      new CachedSchemaCompiler.Impl[Decoder] {
        protected override type Aux[A] = UrlFormDataDecoder[A]
        override def fromSchema[A](
            schema: Schema[A],
            cache: Cache
        ): Decoder[A] = {
          val schemaVisitor = new UrlFormDataDecoderSchemaVisitor(
            cache,
            ignoreUrlFormFlattened,
            capitalizeStructAndUnionMemberNames
          )
          val urlFormDataDecoder = schemaVisitor(schema)
          urlForm =>
            urlFormDataDecoder.decode(
              UrlFormCursor(PayloadPath.root, urlForm.values)
            )
        }
      }
  }

  trait Encoder[A] {
    def encode(a: A): UrlForm
  }

  object Encoder {
    def apply(
        capitalizeStructAndUnionMemberNames: Boolean
    ): CachedSchemaCompiler[Encoder] =
      new CachedSchemaCompiler.Impl[Encoder] {
        protected override type Aux[A] = UrlFormDataEncoder[A]
        override def fromSchema[A](
            schema: Schema[A],
            cache: Cache
        ): Encoder[A] = {
          val maybeStaticUrlFormData =
            schema.hints.get(internals.StaticUrlFormElements).map {
              _.elements.map { case (key, value) =>
                UrlForm.FormData(PayloadPath(key), Some(value))
              }
            }
          val schemaVisitor = new UrlFormDataEncoderSchemaVisitor(
            cache,
            capitalizeStructAndUnionMemberNames
          )
          val urlFormDataEncoder = schemaVisitor(schema)
          maybeStaticUrlFormData match {
            case Some(staticUrlFormData) =>
              value =>
                UrlForm(staticUrlFormData ++ urlFormDataEncoder.encode(value))
            case None => value => UrlForm(urlFormDataEncoder.encode(value))
          }

        }
      }

    val toWriterK: PolyFunction[Encoder, Writer[Any, UrlForm, *]] =
      new PolyFunction[Encoder, Writer[Any, UrlForm, *]] {
        def apply[A](fa: Encoder[A]): Writer[Any, UrlForm, A] =
          Writer.encodeBy(fa.encode(_))
      }
  }
}
