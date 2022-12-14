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

import org.http4s.Uri
import cats.implicits._

import java.nio.charset.StandardCharsets
import scala.collection.immutable.{ListMap}

package object internals {

  // Due to AWS's usage of integer as the canonical representation of a Timestamp in smithy , we need to provide the decoder with instructions to use a Long instead.
  // therefore the timestamp type is switched to type epochSeconds: Long
  // This is just a workaround thats limited to testing scenarios
  def mapAllTimestampsToEpoch[A](schema: Schema[A]): Schema[A] = {
    schema.transformHintsTransitively(h =>
      h.++(Hints(smithy.api.TimestampFormat.EPOCH_SECONDS.widen))
    )
  }
  
  def splitQuery(queryString: String): (String, String) = {
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

  def parseQueryParams(
      queryParams: Option[List[String]]
  ): ListMap[String, List[String]] = {
    queryParams.combineAll
      .map(splitQuery)
      .foldRight[ListMap[String, List[String]]](ListMap.empty) {
        case ((k, v), acc) =>
          acc.get(k) match {
            case Some(value) => acc + (k -> (v :: value))
            case None        => acc + (k -> List(v))
          }
      }
  }
}
