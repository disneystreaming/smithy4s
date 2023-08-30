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
import smithy4s.client.UnaryLowLevelClient

// scalafmt: { maxColumn = 120 }
object UnaryClientEndpoint {

  def apply[F[_], Request, Response, I, E, O, SI, SO](
      lowLevelClient: UnaryLowLevelClient[F, Request, Response],
      clientCodecs: UnaryClientCodecs[F, Request, Response, I, E, O],
      isSuccessful: Response => Boolean
  )(implicit F: MonadThrowLike[F]): (I => F[O]) = {

    import clientCodecs._
    def inputToRequest(input: I): F[Request] =
      inputEncoder(input)

    def outputFromResponse(response: Response): F[O] =
      if (isSuccessful(response)) outputDecoder(response)
      else
        F.flatMap(errorDecoder(response))(F.raiseError[O](_))

    (input: I) =>
      F.flatMap(inputToRequest(input)) { request =>
        lowLevelClient.run(request)(outputFromResponse)
      }
  }

}
