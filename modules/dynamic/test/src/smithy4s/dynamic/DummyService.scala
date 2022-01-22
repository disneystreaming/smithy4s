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
package dynamic

import cats.Applicative

object DummyService {

  def apply[F[_]]: PartiallyApplied[F] = new PartiallyApplied[F]

  class PartiallyApplied[F[_]] {
    def create[Alg[_[_, _, _, _, _]], Op[_, _, _, _, _]](implicit
        service: Service[Alg, Op],
        F: Applicative[F]
    ): smithy4s.Monadic[Alg, F] = {
      service.transform[GenLift[F]#λ] {
        service.opToEndpoint.andThen[GenLift[F]#λ](
          new Transformation[Endpoint[Op, *, *, *, *, *], GenLift[F]#λ] {
            def apply[I, E, O, SI, SO](
                ep: Endpoint[Op, I, E, O, SI, SO]
            ): F[O] =
              F.pure(ep.output.compile(DefaultSchematic))
          }
        )
      }
    }
  }

}
