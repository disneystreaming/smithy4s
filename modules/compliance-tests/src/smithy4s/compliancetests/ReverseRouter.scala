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

package smithy4s.compliancetests

import smithy4s.Service
import cats.effect.Resource
import smithy4s.kinds.FunctorAlgebra
import smithy4s.http.CodecAPI
import smithy4s.ShapeTag
import org.http4s.HttpApp

/* A construct encapsulating the action of turning an http4s route into
 * an an algebra
 */
trait ReverseRouter[F[_]] {
  type Protocol
  def protocolTag: ShapeTag[Protocol]
  def codecs: CodecAPI

  def reverseRoutes[Alg[_[_, _, _, _, _]]](routes: HttpApp[F])(implicit
      service: Service[Alg]
  ): Resource[F, FunctorAlgebra[Alg, F]]
}
