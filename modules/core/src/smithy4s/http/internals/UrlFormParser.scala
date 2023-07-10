/*
 *  Copyright 2023 Disney Streaming
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
package internals

import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.nio.CharBuffer
import scala.collection.immutable.BitSet
import scala.collection.mutable.Builder
import scala.io.Codec
import scala.util.control.NoStackTrace
import scala.util.control.NonFatal
import smithy4s.codecs.PayloadPath

/** Split an encoded query string into unencoded key value pairs
  * It always assumes any input is a  valid query, including "".
  * If "" should be interpreted as no query that __MUST__ be
  * checked beforehand.
  */
// TODO: Move into UrlForm?
private[smithy4s] class UrlFormParser(
    codec: Codec,
    colonSeparators: Boolean,
    qChars: BitSet = UrlFormParser.ExtendedQChars
) {
  import UrlFormParser._

  /** Decodes the input into key value pairs.
    * `flush` signals that this is the last input
    */
  def decode(input: CharBuffer, flush: Boolean): ParseResult[UrlForm] =
    decodeVector(input, flush).map(values =>
      UrlForm(UrlForm.FormData.MultipleValues(values))
    )

  /** Decodes the input into key value pairs.
    * `flush` signals that this is the last input
    */
  def decodeVector(
      input: CharBuffer,
      flush: Boolean
  ): ParseResult[Vector[UrlForm.FormData.PathedValue]] = {
    val acc: Builder[UrlForm.FormData.PathedValue, Vector[
      UrlForm.FormData.PathedValue
    ]] =
      Vector.newBuilder
    decodeBuffer(
      input,
      (k, v) => acc += (UrlForm.FormData.PathedValue(k, v)),
      flush
    ) match {
      case Some(e) =>
        ParseResult.fail("Decoding of url encoded data failed.", e)
      case None => ParseResult.success(acc.result())
    }
  }

  // Some[String] represents an error message, None = success
  private def decodeBuffer(
      input: CharBuffer,
      acc: (
          PayloadPath,
          String
      ) => Builder[UrlForm.FormData.PathedValue, Vector[
        UrlForm.FormData.PathedValue
      ]],
      flush: Boolean
  ): Option[String] = {
    val valAcc = new StringBuilder(InitialBufferCapactiy)

    var error: String = null
    var key: String = null
    var state: State = KEY

    def appendValue(): Unit = {
      if (state == KEY) {
        val s = valAcc.result()
        val k = decodeParam(s)
        // TODO: Why don't we null key here?
        val payloadPath = PayloadPath.fromString(k)
        valAcc.clear()
        acc(payloadPath, "")
      } else {
        val k = decodeParam(key)
        key = null
        val payloadPath = PayloadPath.fromString(k)
        val s = valAcc.result()
        valAcc.clear()
        val v = decodeParam(s)
        acc(payloadPath, v)
      }
      ()
    }

    def endPair(): Unit = {
      if (!flush) input.mark()
      appendValue()
      state = KEY
    }

    if (!flush) input.mark()

    // begin iterating through the chars
    while (error == null && input.hasRemaining) {
      val c = input.get()
      c match {
        case '&' => endPair()

        case ';' if colonSeparators => endPair()

        case '=' =>
          if (state == VALUE) valAcc.append('=')
          else {
            state = VALUE
            key = valAcc.result()
            valAcc.clear()
          }

        case c if qChars.contains(c.toInt) => valAcc.append(c)

        case c => error = s"Invalid char while splitting key/value pairs: '$c'"
      }
    }
    if (error != null) Some(error)
    else {
      if (flush) appendValue()
      else input.reset() // rewind to the last mark position
      None
    }
  }

  private def decodeParam(str: String): String =
    try URLDecoder.decode(
      str,
      codec.charSet.name
      // TODO: Check this doesn't matter!
      // , plusIsSpace = true
    )
    catch {
      case _: IllegalArgumentException     => ""
      case _: UnsupportedEncodingException => ""
    }
}

private[smithy4s] object UrlFormParser {

  final case class ParseFailure(sanitized: String, details: String)
      extends NoStackTrace {
    def message: String =
      if (sanitized.isEmpty) details
      else if (details.isEmpty) sanitized
      else s"$sanitized: $details"

    def cause: Option[Throwable] = None
  }

  // TOOD: Inline
  type ParseResult[+A] = Either[ParseFailure, A]

  object ParseResult {
    def fail(sanitized: String, details: String): ParseResult[Nothing] =
      Left(ParseFailure(sanitized, details))

    def success[A](a: A): ParseResult[A] =
      Right(a)

    def fromTryCatchNonFatal[A](sanitized: String)(f: => A): ParseResult[A] =
      try ParseResult.success(f)
      catch {
        case NonFatal(e) => Left(ParseFailure(sanitized, e.getMessage))
      }
  }

  private val InitialBufferCapactiy = 32

  def parseUrlForm(
      urlForm: String,
      codec: Codec = Codec.UTF8
  ): ParseResult[UrlForm] =
    if (urlForm.isEmpty) Right(UrlForm.empty)
    else new UrlFormParser(codec, true).decode(CharBuffer.wrap(urlForm), true)

  def parseUrlFormVector(
      urlForm: String,
      codec: Codec = Codec.UTF8
  ): ParseResult[Vector[UrlForm.FormData.PathedValue]] =
    if (urlForm.isEmpty) Right(Vector.empty)
    else
      new UrlFormParser(codec, true)
        .decodeVector(CharBuffer.wrap(urlForm), true)

  private sealed trait State
  private case object KEY extends State
  private case object VALUE extends State

  /** Defines the characters that are allowed unquoted within a query string as
    * defined in RFC 3986
    */
  val QChars: BitSet = BitSet(
    (Pchar ++ "/?".toSet - '&' - '=').map(_.toInt).toSeq: _*
  )

  /** PHP also includes square brackets ([ and ]) with query strings. This goes
    * against the spec but due to PHP's widespread adoption it is necessary to
    * support this extension.
    */
  val ExtendedQChars: BitSet = QChars ++ ("[]".map(_.toInt).toSet)
  private def Pchar = Unreserved ++ SubDelims ++ ":@%".toSet
  private def Unreserved = "-._~".toSet ++ AlphaNum
  private def SubDelims = "!$&'()*+,;=".toSet
  private def AlphaNum = (('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')).toSet
}
