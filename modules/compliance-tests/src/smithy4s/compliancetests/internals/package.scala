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

import org.http4s.{Header, Headers, Uri}
import cats.implicits._
import org.typelevel.ci.CIString
import java.nio.charset.StandardCharsets
import scala.collection.immutable.ListMap
import cats.data.Chain

package object internals {

  private[compliancetests] def splitQuery(
      queryString: String
  ): (String, String) = {
    queryString.split("=", 2) match {
      case Array(k, v) =>
        (
          k,
          Uri.decode(
            toDecode = v,
            charset = StandardCharsets.UTF_8,
            plusIsSpace = true
          )
        )
      case Array(k) => (k, "")
    }
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

  private def escape(str: String): String = {
    val withEscapedQuotes = str.replace("\"", "\\\"")
    if (str.contains(",")) {
      "\"" + withEscapedQuotes + "\""
    } else withEscapedQuotes
  }

  /**
   * If there's a single value for a given key, injects in the map without changes.
   * If there a multiple values for a given key, escape each value, escape quotes, then add quotes.
   */
  private[compliancetests] def collapseHeaders(
      headers: Headers
  ): Map[String, String] = {
    def append(
        acc: Map[String, List[String]],
        key: CIString,
        newValue: String
    ) = {

      (key.toString -> acc
        .get(key.toString)
        .map(existing => existing :+ newValue)
        .getOrElse(List(newValue)))
    }

    val multimap = headers.headers.foldLeft(Map.empty[String, List[String]]) {
      case (acc, Header.Raw(key, newValue)) =>
        acc + append(acc, key, newValue)
    }
    multimap.collect {
      case (key, value :: Nil) => (key, value)
      case (key, values) if values.size > 1 =>
        (key, values.map(escape).mkString(", "))
    }

  }

  /*
   This function takes a string and splits it on a comma delimiter and prunes extra whitespace which
   what makes it a bit more complicated is we need to keep track of if we are in an open quote or not
   */
  private[compliancetests] def parseList(s: String): List[String] = {
    s.foldLeft((Chain.empty[String], 0, 0, false)) {
      case ((acc, begin, end, quote), elem) =>
        elem match {
          // we are in a quote so we negate the quote state and move on
          case '"' => (acc, begin, end + 1, !quote)
          // we see a comma, we are not in a quote if we actually have some data,  we add the current string to the accumulator and move both beginning and end pointers to the next character otherwise we move along
          case ',' if !quote =>
            if (begin < end)
              (acc :+ s.substring(begin, end), end + 1, end + 1, quote)
            else (acc, end + 1, end + 1, quote)
          // we see a whitespace character and we have not captured any data yet so we move the beginning pointer and end pointer to the next character
          case c if c.isWhitespace && begin == end =>
            (acc, begin + 1, begin + 1, quote)
          // default case if we have reached the end of the string , we must have some data we add it to the accumulator else we just increment the end pointer
          case _ =>
            if (s.length == end + 1)
              (acc :+ s.substring(begin, end + 1), end + 1, end + 1, quote)
            else {
              (acc, begin, end + 1, quote)
            }
        }
    }._1
      .toList
  }

}
