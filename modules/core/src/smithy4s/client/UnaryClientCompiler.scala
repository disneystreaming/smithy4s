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

import smithy4s.Endpoint
import smithy4s.capability.MonadThrowLike

object UnaryClientCompiler {

  def apply[Alg[_[_, _, _, _, _]], F[_], Client, Request, Response](
      service: smithy4s.Service[Alg],
      client: Client,
      toSmithy4sClient: Client => UnaryLowLevelClient[F, Request, Response],
      makeClientCodecs: UnaryClientCodecs.Make[F, Request, Response],
      middleware: Endpoint.Middleware[Client],
      isSuccessful: Response => Boolean
  )(implicit F: MonadThrowLike[F]): service.FunctorEndpointCompiler[F] =
    new service.FunctorEndpointCompiler[F] {
      def apply[I, E, O, SI, SO](
          endpoint: service.Endpoint[I, E, O, SI, SO]
      ): I => F[O] = {

        val transformedClient =
          middleware.prepare(service)(endpoint).apply(client)

        val adaptedClient = toSmithy4sClient(transformedClient)

        UnaryClientEndpoint(
          adaptedClient,
          makeClientCodecs(endpoint),
          isSuccessful
        )
      }
    }

}
