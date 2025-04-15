/*
 *  Copyright 2021-2025 Disney Streaming
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

import smithy4s.schema.OperationSchema
import smithy.api.Http
import smithy4s.http.internals.SchemaVisitorPathEncoder
import scala.annotation.tailrec

trait HttpEndpoint[I] {
  // Returns a list of path segments that should be appended to the base URL. These are not URL-encoded.
  @deprecated(
    "Path segments should be encoded in a specific way per the Smithy spec, so use encodedPath instead",
    "0.18.40"
  )
  def path(input: I): List[String]

  def encodedPath(input: I): List[String]

  // Returns a path template as a list of segments, which can be constant strings or placeholders.
  def path: List[PathSegment]

  // Returns a map of static query parameters that are found in the uri of Http hint.
  def staticQueryParams: Map[String, Seq[String]]
  def method: HttpMethod
  def code: Int

  final def matches(rPath: IndexedSeq[String]): Option[Map[String, String]] =
    matchPath(path, rPath)
}

object HttpEndpoint {

  def unapply[I, E, O, SI, SO](
      operation: OperationSchema[I, E, O, SI, SO]
  ): Option[HttpEndpoint[I]] = cast(operation).toOption

  def cast[I, E, O, SI, SO](
      operation: OperationSchema[I, E, O, SI, SO]
  ): Either[HttpEndpointError, HttpEndpoint[I]] = {
    for {
      http <- operation.hints
        .get(Http)
        .toRight(HttpEndpointError("Operation doesn't have a @http trait"))
      httpMethod = HttpMethod.fromStringOrDefault(http.method.value)
      queryParams = internals.staticQueryParams(http.uri.value)
      httpPath <- internals
        .pathSegments(http.uri.value)
        .toRight(
          HttpEndpointError(
            s"Unable to parse HTTP path template: ${http.uri.value}"
          )
        )

      labelEncodingEncoder <- new SchemaVisitorPathEncoder(
        urlEncodeHttpLabelValues = true
      )(
        operation.input.addHints(http)
      ).toRight(
        HttpEndpointError("Unable to encode operation input in HTTP path")
      )
      nonLabelEncodingEncoder <- new SchemaVisitorPathEncoder(
        urlEncodeHttpLabelValues = false
      )(
        operation.input.addHints(http)
      ).toRight(
        HttpEndpointError("Unable to encode operation input in HTTP path")
      )

    } yield {
      new HttpEndpoint[I] {
        def path(input: I): List[String] = nonLabelEncodingEncoder.encode(input)

        def encodedPath(input: I): List[String] =
          labelEncodingEncoder.encode(input)
        val staticQueryParams: Map[String, Seq[String]] = queryParams
        val path: List[PathSegment] = httpPath.toList
        val method: HttpMethod = httpMethod
        val code: Int = http.code
      }
    }
  }

  case class HttpEndpointError(message: String) extends Exception(message)

  /**
   * Returns true if "left" is more or equally specific to "right", according to https://smithy.io/2.0/spec/http-bindings.html#specificity-routing

   * The following algorithm is used to compare two paths
   *
   * Given two ambiguous URI patterns A and B with segments [A0, …, An] and [B0, …, Bm] with query string literals [AQ0, …, AQp] and [BQ0, …, BQq] (with both p
   * and q possibly zero, i.e., without query string literals), the following steps are taken to compare them, for each index x from 0 to min(n, m)
   *
   * If A[x] and B[x] are both literals then continue (the literal values have to be equal otherwise the patterns are not ambiguous) If A[x] is a literal and
   * B[x] is a label then A is more specific than B, If A[x] is a non-greedy label and B[x] is a greedy label then A is more specific than B If n > m then A is
   * more specific than B If p > q then A is more specific than B.
   */
  def moreSpecific(left: HttpEndpoint[_], right: HttpEndpoint[_]): Boolean = {
    // If A[x] is a literal and B[x] is a label then A is more specific than B
    // If A[x] is a non-greedy label and B[x] is a greedy label then A is more specific than B
    val weight: PathSegment => Int = {
      case _: PathSegment.StaticSegment => 0
      case _: PathSegment.LabelSegment  => 1
      case _: PathSegment.GreedySegment => 2
    }

    @tailrec
    def pathSegmentListLt(a: List[PathSegment], b: List[PathSegment]): Boolean =
      (a, b) match {
        // If p > q then A is more specific than B
        case (Nil, Nil) =>
          left.staticQueryParams.size > right.staticQueryParams.size
        // If n > m then A is more specific than B
        case (Nil, _) => false
        case (_, Nil) => true
        case (h1 :: t1, h2 :: t2) =>
          val comp = (h1, h2) match {
            case (PathSegment.StaticSegment(a), PathSegment.StaticSegment(b)) =>
              a.compare(b)
            case (x, y) => weight(x) - weight(y)
          }
          if (comp == 0) {
            // If A[x] and B[x] are both literals then continue (the literal values have to be equal otherwise the patterns are not ambiguous)
            pathSegmentListLt(t1, t2)
          } else {
            comp < 0
          }
      }

    pathSegmentListLt(left.path, right.path)
  }
}
