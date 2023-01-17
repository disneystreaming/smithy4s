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

  // Due to AWS's usage of integer as the canonical representation of a Timestamp in smithy , we need to provide the decoder with instructions to use a Long instead.
  // therefore the timestamp type is switched to type epochSeconds: Long
  // This is just a workaround thats limited to testing scenarios
  private[compliancetests] def mapAllTimestampsToEpoch[A](
      schema: Schema[A]
  ): Schema[A] = {
    schema.transformHintsTransitively(h =>
      h.++(Hints(smithy.api.TimestampFormat.EPOCH_SECONDS.widen))
    )
  }

  // a HintMask to hold onto hints that are necessary for correct document decoding
  private val awsMask = HintMask(IntEnum)

  private[compliancetests] implicit class SchemaOps[A](val schema: Schema[A])
      extends AnyVal {

    def awsHintMask: Schema[A] =
      schema.transformHintsTransitively(awsMask.apply)
  }

  private def splitQuery(queryString: String): (String, String) = {
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

  // splits string into List utilizing a comma that is not between quotes as quotes can be used to escape commas in headers
  private def parseList(s: String): List[String] = {
    s.foldLeft((Chain.empty[String], 0, 0, false)) {
      case ((acc, begin, end, quote), elem) =>
        elem match {
          case '"' => (acc, begin, end + 1, !quote)
          case ',' if !quote && begin < end =>
            (acc :+ s.substring(begin, end), end + 1, end + 1, quote)
          case c if c.isWhitespace && begin == end =>
            (acc, begin + 1, end + 1, quote)
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
