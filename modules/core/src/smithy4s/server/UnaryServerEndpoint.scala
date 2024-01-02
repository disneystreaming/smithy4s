/*
 *  Copyright 2021-2024 Disney Streaming
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
package server

import smithy4s.capability.MonadThrowLike

import smithy4s.kinds._

// scalafmt: {maxColumn: 120}
object UnaryServerEndpoint {

  def apply[F[_], Op[_, _, _, _, _], Request, Response, I, E, O, SI, SO](
      interpreter: FunctorInterpreter[Op, F],
      endpoint: Endpoint[Op, I, E, O, SI, SO],
      codecs: UnaryServerCodecs[F, Request, Response, I, E, O],
      middleware: (Request => F[Response]) => (Request => F[Response])
  )(implicit F: MonadThrowLike[F]): Request => F[Response] = {
    def errorResponse(throwable: Throwable): F[Response] = throwable match {
      case endpoint.Error((_, e)) =>
        codecs.errorEncoder(e)
      case e: Throwable =>
        codecs.throwableEncoder(e)
    }

    val base = { (req: Request) =>
      F.flatMap(codecs.inputDecoder(req)) { input =>
        F.flatMap(interpreter(endpoint.wrap(input))) {
          codecs.outputEncoder
        }
      }
    }
    val withMiddleware = middleware(base)
    withMiddleware.andThen { F.handleErrorWith(_)(errorResponse) }

  }

}
