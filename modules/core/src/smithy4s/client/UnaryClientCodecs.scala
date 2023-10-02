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

package smithy4s.client

import smithy4s.capability.MonadThrowLike
import smithy4s.schema.OperationSchema

// scalafmt: {maxColumn = 120}
final class UnaryClientCodecs[F[_], Request, Response, I, E, O](
    val inputEncoder: I => F[Request],
    val errorDecoder: Response => F[Throwable],
    val outputDecoder: Response => F[O]
) {

  def transformResponse[Response0](f: Response0 => F[Response])(implicit
      F: MonadThrowLike[F]
  ): UnaryClientCodecs[F, Request, Response0, I, E, O] = {
    new UnaryClientCodecs(inputEncoder, f.andThen(F.flatMap(_)(errorDecoder)), f.andThen(F.flatMap(_)(outputDecoder)))
  }

  def transformRequest[Request1](
      f: Request => F[Request1]
  )(implicit F: MonadThrowLike[F]): UnaryClientCodecs[F, Request1, Response, I, E, O] = {
    new UnaryClientCodecs(inputEncoder.andThen(F.flatMap(_)(f)), errorDecoder, outputDecoder)
  }

}

object UnaryClientCodecs {

  trait Make[F[_], Request, Response] { self =>
    def apply[I, E, O, SI, SO](
        schema: OperationSchema[I, E, O, SI, SO]
    ): UnaryClientCodecs[F, Request, Response, I, E, O]

    final def transformResponse[Response0](
        f: Response0 => F[Response]
    )(implicit F: MonadThrowLike[F]): Make[F, Request, Response0] = new Make[F, Request, Response0] {
      def apply[I, E, O, SI, SO](
          schema: OperationSchema[I, E, O, SI, SO]
      ): UnaryClientCodecs[F, Request, Response0, I, E, O] = self(schema).transformResponse(f)
    }

    final def transformRequest[Request1](
        f: Request => F[Request1]
    )(implicit F: MonadThrowLike[F]): Make[F, Request1, Response] = new Make[F, Request1, Response] {
      def apply[I, E, O, SI, SO](
          schema: OperationSchema[I, E, O, SI, SO]
      ): UnaryClientCodecs[F, Request1, Response, I, E, O] = self(schema).transformRequest(f)
    }
  }

}
