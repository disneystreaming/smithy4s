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

package smithy4s.server

import smithy4s.schema.OperationSchema
import smithy4s.capability.MonadThrowLike

// scalafmt: {maxColumn = 120}
final class UnaryServerCodecs[F[_], Request, Response, I, E, O](
    val inputDecoder: Request => F[I],
    val errorEncoder: E => F[Response],
    val throwableEncoder: Throwable => F[Response],
    val outputEncoder: O => F[Response]
) {

  def transformRequest[Request0](f: Request0 => F[Request])(implicit
      F: MonadThrowLike[F]
  ): UnaryServerCodecs[F, Request0, Response, I, E, O] = {
    new UnaryServerCodecs(
      f.andThen(F.flatMap(_)(inputDecoder)),
      errorEncoder,
      throwableEncoder,
      outputEncoder
    )
  }

  def transformResponse[Response1](
      f: Response => F[Response1]
  )(implicit F: MonadThrowLike[F]): UnaryServerCodecs[F, Request, Response1, I, E, O] = {
    new UnaryServerCodecs(
      inputDecoder,
      errorEncoder.andThen(F.flatMap(_)(f)),
      throwableEncoder.andThen(F.flatMap(_)(f)),
      outputEncoder.andThen(F.flatMap(_)(f))
    )
  }

}

object UnaryServerCodecs {

  type For[F[_], Request, Response] = {
    type toKind5[I, E, O, SI, SO] =
      UnaryServerCodecs[F, Request, Response, I, E, O]
  }

  type Make[F[_], Request, Response] =
    smithy4s.kinds.PolyFunction5[OperationSchema, For[F, Request, Response]#toKind5]

}
