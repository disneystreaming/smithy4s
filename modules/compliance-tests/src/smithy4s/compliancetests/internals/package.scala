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
package compliancetests

import org.http4s.{Header, Headers, Uri, HttpDate}
import cats.implicits._
import cats.data.Chain
import org.typelevel.ci.CIString
import java.nio.charset.StandardCharsets
import scala.collection.immutable.ListMap

package object internals {

  private[compliancetests] def splitQuery(
      queryString: String
  ): (String, String) = {
    queryString.split("=", 2) match {
      case Array(k, v) =>
        (
          k,
          decodeUri(v)
        )
      case Array(k) => (k, "")
    }
  }

   def decodeUri(v: String) = {
    Uri.decode(
      toDecode = v,
      charset = StandardCharsets.UTF_8
    )
  }

  private[compliancetests] def parseQueryParams(
      queryParams: Option[List[String]]
  ): ListMap[String, List[String]] = {
    queryParams.combineAll
      .map(splitQuery)
      .foldLeft[ListMap[String, List[String]]](ListMap.empty) {
        case (acc, (k, v)) =>
          acc.get(k) match {
            case Some(value) => acc + (k -> (value :+ v))
            case None        => acc + (k -> List(v))
          }
      }
  }

  private[compliancetests] def parseHeaders(
      maybeHeaders: Option[Map[String, String]]
  ): Headers =
    maybeHeaders.fold(Headers.empty)(h =>
      Headers(h.toList.flatMap(parseSingleHeader).map(a => a: Header.ToRaw): _*)
    )

  private def parseSingleHeader(
      kv: (String, String)
  ): List[(String, String)] = {
    kv match {
      case (k, v) => parseList(v).map((k, _))
    }
  }


  /**
   * The idea behind this parser is to be able to take a string that is formatted as a comma delimited list literal and parse it into a list of strings
   * The parser is designed to handle the following cases
   * - escaped characters
   * - whitespace between list items are removed
   * - commas between list items are removed
   * - quotes are removed when used to escape a comma
   * @param input
   * @return
   */

  private[compliancetests] def parseList(input: String): List[String] = {
    @scala.annotation.tailrec
    def loop(
        chars: List[Char],
        result: Chain[String],
        inString: Boolean,
        escapeNext: Boolean,
        betweenItems: Boolean,
        currentString: String
    ): Chain[String] = {
      chars match {
        // We are at the end of the string, if we are in a string we need to add the current string to the result otherwise we are done
        case Nil =>
          if (currentString.nonEmpty)
            result :+ currentString
          else
            result
        // If we see a quote we need to check if it is escaped or not, if it is espaced we need to add it to the current string and maintain inString state otherwise we need to toggle inString state and skip appending the quote to the current string
        case '"' :: tail  =>
          if(escapeNext)
            loop(tail, result, inString, false, betweenItems, currentString + '"')
          else
            loop(tail, result, !inString, false, betweenItems, currentString)
       // If we see a comma and we are not in a string , this signals a separate key value pair for the header
        // if the current string is not empty we need to add it to the result otherwise skip it
        // we also set the state to be between items in a list
        case ',' :: tail if !inString =>
          if (currentString.nonEmpty)
            loop(tail, result :+ currentString, false, false, true, "")
          else {
            loop(tail, result, inString, false, betweenItems, "")
          }
        // If we see a backslash and we are not in an escaped state , we need to set the escapeNext flag to true and skip appending the backslash to the current string
        case '\\' :: tail if !escapeNext =>
          loop(tail, result, inString, true, betweenItems, currentString)

        // this case handles all whitespace characters,
        // if we are  not between items in a list we need to add the current string to the result and reset the current string
        case char :: tail if char.isWhitespace =>
          if (!betweenItems)
            loop(
              tail,
              result,
              inString,
              false,
              betweenItems,
              currentString + char
            )
          else
            loop(tail, result, inString, false, betweenItems, currentString)

          // default case is we just append the current character to the current string
        case char :: tail =>
          loop(
            tail,
            result,
            inString,
            false,
            betweenItems,
            currentString + char
          )
      }
    }
    loop(input.toList, Chain.empty[String], false, false, false, "").toList

  }

  /**
   * if a String in the List happens to be a valid http-date timestamp, then concatenate as is
   * else if String has commas in it, escape the quotes inside the String and add quotes before concatenating
   * else escape quotes inside the String concatenate it
   *
   * @param headers
   * @return
   */
  private[compliancetests] def collapseHeaders(
                                                headers: Headers
                                              ): Map[String, String] = {
    def append(acc: Map[String, String], key: CIString, expectedValue: String) = {
      (key.toString -> acc.get(key.toString).map(e => s"$e, $expectedValue").getOrElse(expectedValue))
    }

    headers.headers.foldLeft(Map.empty[String, String]) {
      case (acc, Header.Raw(key, expectedValue))
        if HttpDate.fromString(expectedValue).isRight =>
        acc + append(acc, key, expectedValue)
      case (acc, Header.Raw(key, expectedValue)) =>
        val escapeQuotes = expectedValue.replaceAll("\"", "\\\\\"")
        val replaceEscapes = escapeQuotes.replaceAll("\\\\", "\\\\\\\\")
        if (expectedValue.contains(","))
          acc + append(acc, key, s"""\"$replaceEscapes\"""")
        else {
          acc + append(acc, key, escapeQuotes)
        }
    }
  }
}
