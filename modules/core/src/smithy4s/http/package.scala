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

import smithy4s.codecs.Reader

package object http {

  val errorTypeHeader = "X-Error-Type"
  val amazonErrorTypeHeader = "X-Amzn-Errortype"

  type HttpPayloadReader[A] = Reader[Either[HttpContractError, *], Blob, A]

  type PathParams = Map[String, String]
  type HttpMediaType = HttpMediaType.Type

  final def httpMatch[Alg[_[_, _, _, _, _]]](
      service: Service[Alg],
      method: http.HttpMethod,
      path: String
  ): Option[
    (
        service.Endpoint[_, _, _, _, _],
        http.HttpEndpoint[_],
        Map[String, String]
    )
  ] = httpMatch(
    service,
    method,
    pathSegments = matchPath.make(path).toIndexedSeq
  )

  /**
    * Returns the first http endpoint that matches both a method and path, as well as the map
    * of extracted segment values.
    */
  final def httpMatch[Alg[_[_, _, _, _, _]]](
      service: Service[Alg],
      method: http.HttpMethod,
      pathSegments: IndexedSeq[String]
  ): Option[
    (
        service.Endpoint[_, _, _, _, _],
        http.HttpEndpoint[_],
        Map[String, String]
    )
  ] = {
    service.endpoints.iterator
      .map {
        case endpoint @ http.HttpEndpoint(httpEndpoint)
            if httpEndpoint.method == method =>
          httpEndpoint
            .matches(pathSegments)
            .map(metadata => (endpoint, httpEndpoint, metadata))
        case _ => None
      }
      .find(_.isDefined)
      .flatten
  }

}
