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

import smithy4s.schema.OperationSchema
import smithy.api.Http
import smithy4s.http.internals.SchemaVisitorPathEncoder

trait HttpEndpoint[I] {
  // Returns a list of path segments that should be appended to the base URL. These are not URL-encoded.
  def path(input: I): List[String]

  // Returns a path template as a list of segments, which can be constant strings or placeholders.
  def path: List[PathSegment]

  // Returns a map of static query parameters that are found in the uri of Http hint.
  def staticQueryParams: Map[String, Seq[String]]
  def method: HttpMethod
  def code: Int

  final def matches(rPath: Array[String]): Option[Map[String, String]] =
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

      encoder <- SchemaVisitorPathEncoder(
        operation.input.addHints(http)
      ).toRight(
        HttpEndpointError("Unable to encode operation input in HTTP path")
      )

    } yield {
      new HttpEndpoint[I] {
        def path(input: I): List[String] = encoder.encode(input)
        val staticQueryParams: Map[String, Seq[String]] = queryParams
        val path: List[PathSegment] = httpPath.toList
        val method: HttpMethod = httpMethod
        val code: Int = http.code
      }
    }
  }

  case class HttpEndpointError(message: String) extends Exception(message)

}
