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

import cats.effect.Resource
import org.http4s.HttpRoutes
import smithy4s.Service
import smithy4s.ShapeTag
import smithy4s.kinds.FunctorAlgebra

/* A construct encapsulating the action of turning an algebra implementation into
 * an http route (modelled using Http4s, but could be backed by any other library
 * by means of proxyfication)
 */
trait Router[F[_]] {
  type Protocol
  def protocolTag: ShapeTag[Protocol]

  def routes[Alg[_[_, _, _, _, _]]](alg: FunctorAlgebra[Alg, F])(implicit
      service: Service[Alg]
  ): Resource[F, HttpRoutes[F]]
}
