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
  ): Option[HttpEndpoint[I]] = endpoint match {
    case he: HttpEndpoint[_] => Some(he.asInstanceOf[HttpEndpoint[I]])
    case _                   => None
  }
}
