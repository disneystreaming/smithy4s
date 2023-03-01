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
import smithy4s.kinds.PolyFunction5

import java.nio.charset.StandardCharsets
import scala.collection.immutable.ListMap

package object internals {

  // Due to AWS's usage of integer as the canonical representation of a Timestamp in smithy , we need to provide the decoder with instructions to use a Long instead.
  // therefore the timestamp type is switched to type epochSeconds: Long
  // This is just a workaround thats limited to testing scenarios

  private[compliancetests] implicit class EndpointSchemaMapHints[Op[
      _,
      _,
      _,
      _,
      _
  ], I, E, O, SI, SO](endpoint: Endpoint[Op, I, E, O, SI, SO]) {

    def mapHints(
        func: MappedHintsSchemaVisitor
    ): Endpoint[Op, I, E, O, SI, SO] = {
      new Endpoint[Op, I, E, O, SI, SO] {
        override def id: ShapeId = endpoint.id

        override def input: Schema[I] = func(endpoint.input)

        override def output: Schema[O] = func(endpoint.output)

        override def streamedInput: StreamingSchema[SI] = endpoint.streamedInput

        override def streamedOutput: StreamingSchema[SO] =
          endpoint.streamedOutput

        override def hints: Hints = endpoint.hints

        def wrap(input: I) = endpoint.wrap(input)

        override def errorable: Option[Errorable[E]] = endpoint.errorable.map {
          errorable =>
            new Errorable[E] {
              def error: schema.Schema.UnionSchema[E] =
                func(errorable.error).asInstanceOf[schema.Schema.UnionSchema[E]]

              def liftError(throwable: Throwable): Option[E] =
                errorable.liftError(throwable)

              def unliftError(e: E): Throwable = errorable.unliftError(e)

            }
        }
      }
    }
  }

  private[smithy4s] def transformService[Alg[_[_, _, _, _, _]]](
      that: Service[Alg]
  )(func: Hints => Hints): Service[Alg] = {
    val visitor = new MappedHintsSchemaVisitor(func)

    new Service[Alg] {

      override type Operation[I, E, O, SI, SO] = that.Operation[I, E, O, SI, SO]

      val cache
          : Map[ShapeId, smithy4s.Endpoint[that.Operation, _, _, _, _, _]] =
        that.endpoints.map(_.mapHints(visitor)).map(e => e.id -> e).toMap

      override def endpoints: List[Endpoint[_, _, _, _, _]] =
        cache.values.toList

      override def endpoint[I, E, O, SI, SO](
          op: that.Operation[I, E, O, SI, SO]
      ): (I, Endpoint[I, E, O, SI, SO]) =
        that.endpoint(op) match {
          case (i, e) =>
            (i, cache(e.id).asInstanceOf[Endpoint[I, E, O, SI, SO]])
        }

      override def fromPolyFunction[P[_, _, _, _, _]](
          function: PolyFunction5[that.Operation, P]
      ): Alg[P] = that.fromPolyFunction(function)

      override def version: String = that.version

      override def hints: Hints = that.hints

      override def reified: Alg[Operation] = that.reified

      override def toPolyFunction[P[_, _, _, _, _]](
          algebra: Alg[P]
      ): PolyFunction5[Operation, P] = that.toPolyFunction(algebra)

      override def mapK5[F[_, _, _, _, _], G[_, _, _, _, _]](
          alg: Alg[F],
          function: PolyFunction5[F, G]
      ): Alg[G] = that.mapK5(alg, function)

      override def id: ShapeId = that.id
    }
  }
  type HintMapper = Hints => Hints

  private[smithy4s] val mapAllTimestampsToEpoch: HintMapper = h => {
    if (
      h.get[smithy.api.TimestampFormat].isEmpty && h
        .get[smithy.api.HttpHeader]
        .isEmpty && h.get[smithy.api.HttpLabel].isEmpty && h
        .get[smithy.api.HttpQuery]
        .isEmpty
      && h.get[smithy.api.HttpQueryParams].isEmpty
    )
      h ++ Hints(smithy.api.TimestampFormat.EPOCH_SECONDS.widen)
    else {
      h
    }
  }

  private[smithy4s] val mapAllTimestampsToEpochDocument: HintMapper =
    h => {
      h ++ Hints(smithy.api.TimestampFormat.EPOCH_SECONDS.widen)
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
