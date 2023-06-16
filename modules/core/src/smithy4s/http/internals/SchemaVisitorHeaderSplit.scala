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

package smithy4s.http.internals

import smithy4s.schema._
import smithy4s.{Hints, ShapeId}
import smithy4s.Bijection
import smithy4s.Refinement
import smithy4s.schema.Primitive.PTimestamp
import smithy.api.TimestampFormat

/**
  * A schema visitor that allows to formulate a function that splits a single header value
  * into multiple ones.
  *
  * See https://github.com/awslabs/smithy/pull/1798
  */
object SchemaVisitorHeaderSplit
    extends SchemaVisitor.Default[AwsHeaderSplitter] {
  self =>

  def default[A]: AwsHeaderSplitter[A] = None

  private def isHttpDate(hints: Hints): Boolean = {
    // Using forall because http headers assume HTTP_DATE format by default.
    // If the Hint isn't there, we're defaulting to HTTP_DATE
    hints.get(TimestampFormat).forall(_ == TimestampFormat.HTTP_DATE)
  }

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): AwsHeaderSplitter[P] = tag match {
    case PTimestamp if isHttpDate(hints) =>
      Some(splitHeaderValue(_, isHttpDate = true))
    case _ => Some(splitHeaderValue(_, isHttpDate = false))
  }
  override def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): AwsHeaderSplitter[B] = schema.compile(self): Option[String => Seq[String]]

  override def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): AwsHeaderSplitter[B] = schema.compile(self): Option[String => Seq[String]]

  override def nullable[A](schema: Schema[A]): AwsHeaderSplitter[Option[A]] =
    schema.compile(self): Option[String => Seq[String]]

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      tag: EnumTag,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): AwsHeaderSplitter[E] = Some(splitHeaderValue(_, isHttpDate = false))

  private[internals] def splitHeaderValue(
      headerValue: String,
      isHttpDate: Boolean
  ): Seq[String] = {

    val totalLength = headerValue.length()
    val entries = Seq.newBuilder[String]
    var i = 0

    def isUnescapedDQuote(): Boolean = {
      headerValue.charAt(i) == '"' &&
      ((i == 0) || headerValue.charAt(i - 1) != '\\')
    }

    def skipUntilUnescapedDQuote(): Unit = {
      while (i < headerValue.length && !isUnescapedDQuote()) {
        i += 1
      }
    }
    def skipUntilCommaOrEnd(): Unit = {
      while (i < headerValue.length() && headerValue.charAt(i) != ',') {
        i += 1
      }
    }

    while (i < totalLength) {
      val char = headerValue.charAt(i)
      if (char.isWhitespace) { i += 1 }
      else if (isUnescapedDQuote()) {
        i += 1
        val start = i
        skipUntilUnescapedDQuote()
        i += 1
        val entry = headerValue.substring(start, i - 1).replace("\\\"", "\"")
        entries += entry
        skipUntilCommaOrEnd()
        i += 1
      } else {
        val start = i
        skipUntilCommaOrEnd()
        if (isHttpDate) {
          i += 1
          skipUntilCommaOrEnd()
        }
        val entry = headerValue.substring(start, i).trim()
        entries += entry
        i += 1
      }
    }
    entries.result()
  }

}
