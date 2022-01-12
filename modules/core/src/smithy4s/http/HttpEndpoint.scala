/*
 *  Copyright 2021 Disney Streaming
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

import smithy4s.syntax._
import smithy.api.Http

trait HttpEndpoint[I] {
  def path(input: I): String
  def path: List[PathSegment]
  def method: HttpMethod
  def code: Int

  final def matches(rPath: Array[String]): Option[Map[String, String]] =
    matchPath(path, rPath)

}

object HttpEndpoint {

  def unapply[Op[_, _, _, _, _], I, E, O, SI, SO](
      endpoint: Endpoint[Op, I, E, O, SI, SO]
  ): Option[HttpEndpoint[I]] = cast(endpoint)

  def cast[Op[_, _, _, _, _], I, E, O, SI, SO](
      endpoint: Endpoint[Op, I, E, O, SI, SO]
  ): Option[HttpEndpoint[I]] = {
    for {
      http <- endpoint.hints.get(Http)
      httpMethod <- HttpMethod.fromString(http.method.value)
      httpPath <- internals.pathSegments(http.uri.value)
      encoder <- endpoint.input
        .withHints(http)
        .compile(internals.SchematicPathEncoder)
        .get
    } yield {
      new HttpEndpoint[I] {
        def path(input: I): String = {
          val sb = new StringBuilder()
          encoder.encode(sb, input)
          sb.result()
        }
        val path: List[PathSegment] = httpPath.toList
        val method: HttpMethod = httpMethod
        val code: Int = http.code.getOrElse(200)
      }
    }
  }

}
