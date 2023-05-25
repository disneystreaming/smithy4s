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
import cats.data.Chain
import java.nio.charset.StandardCharsets
import scala.collection.immutable.ListMap

package object internals {
  private def splitQuery(queryString: String): (String, String) = {
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

  /*
       This function takes a string that is encoded like source code and splits it on a comma delimiter and prunes extra whitespace which
       what makes it a bit more complicated is we need to keep track of if we are in an open quote or not
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
        case Nil =>
          if (currentString.nonEmpty)
            result :+ currentString
          else
            result

        case '"' :: tail if !escapeNext =>
          loop(tail, result, !inString, false, betweenItems, currentString)

        case '"' :: tail if escapeNext =>
          loop(tail, result, inString, false, betweenItems, currentString + '"')

        case ',' :: tail if !inString =>
          if (currentString.nonEmpty)
            loop(tail, result :+ currentString, false, false, true, "")
          else {
            loop(tail, result, inString, false, betweenItems, "")
          }
        case '\\' :: tail if !escapeNext =>
          loop(tail, result, inString, true, betweenItems, currentString)

        case char :: tail if char.isWhitespace =>
          if (!inString && !betweenItems)
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

}
