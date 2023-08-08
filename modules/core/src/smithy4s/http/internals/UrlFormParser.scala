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
package internals

import smithy4s.codecs.PayloadPath

import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import scala.collection.immutable.BitSet

// This is based on http4s' own equivalent, but simplified for our use case.
private[smithy4s] object UrlFormParser {

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
  val QueryChars: BitSet = BitSet(
    (Pchar ++ "/?".toSet - '&' - '=').map(_.toInt).toSeq: _*
  )

  private def Pchar = Unreserved ++ SubDelims ++ ":@%".toSet
  private def Unreserved = "-._~".toSet ++ AlphaNum
  private def AlphaNum = (('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')).toSet
  private def SubDelims = "!$&'()*+,;=".toSet

}
