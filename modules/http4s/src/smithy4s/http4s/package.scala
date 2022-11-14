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

import org.http4s.Header
import org.http4s.Headers
import org.http4s.Media
import org.http4s.{Method => Http4sMethod}
import org.typelevel.ci.CIString
import smithy4s.http.CaseInsensitive
import smithy4s.http.{HttpMethod => SmithyMethod}

package object http4s extends Compat.Package {

  implicit final class ServiceOps[Alg[_[_, _, _, _, _]]](
      private[this] val serviceProvider: smithy4s.Service.Provider[Alg]
  ) {
    @deprecated("this extension method is deprecated ,Use smithy4s.http4s.SimpleRestJsonBuilder")
    def simpleRestJson: SimpleRestJsonBuilder.ServiceBuilder[Alg] =
      SimpleRestJsonBuilder(serviceProvider)

  }

  private[smithy4s] def toHttp4sMethod(method: SmithyMethod): Http4sMethod =
    method match {
      case smithy4s.http.HttpMethod.PUT    => Http4sMethod.PUT
      case smithy4s.http.HttpMethod.POST   => Http4sMethod.POST
      case smithy4s.http.HttpMethod.DELETE => Http4sMethod.DELETE
      case smithy4s.http.HttpMethod.GET    => Http4sMethod.GET
      case smithy4s.http.HttpMethod.PATCH  => Http4sMethod.PATCH
    }

  private[smithy4s] def toHeaders(mp: Map[CaseInsensitive, Seq[String]]) =
    Headers(mp.flatMap { case (k, v) =>
      v.map(Header.Raw(CIString(k.toString), _))
    }.toList)

  private[smithy4s] def getFirstHeader[F[_]](
      req: Media[F],
      s: String
  ): Option[String] =
    req.headers.get(CIString(s)).map(_.head.value)

  private[smithy4s] def toMap(hd: Headers) = hd.headers.map { h =>
    h.name.toString -> h.value
  }.toMap

  private[smithy4s] def getHeaders[F[_]](req: Media[F]) =
    req.headers.headers.groupBy(_.name).map { case (k, v) =>
      (CaseInsensitive(k.toString), v.map(_.value))
    }

}
