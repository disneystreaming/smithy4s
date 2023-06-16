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
  ): Map[CIString, String] = {
    def append(
        acc: Map[CIString, List[String]],
        key: CIString,
        newValue: String
    ) = {

      (key -> acc
        .get(key)
        .map(existing => existing :+ newValue)
        .getOrElse(List(newValue)))
    }

    val multimap = headers.headers.foldLeft(Map.empty[CIString, List[String]]) {
      case (acc, Header.Raw(key, newValue)) =>
        acc + append(acc, key, newValue)
    }
    multimap.collect {
      case (key, value :: Nil) => (key, value)
      case (key, values) if values.size > 1 =>
        (key, values.map(escape).mkString(", "))
    }

  }

}
