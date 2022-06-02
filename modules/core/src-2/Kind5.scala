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

object Kind5 {

  type Existential[F[_, _, _, _, _]] = F[_, _, _, _, _]

  @inline def existential[F[_, _, _, _, _], I, E, O, SI, SO](
      fa: F[I, E, O, SI, SO]
  ): Existential[F] =
    fa.asInstanceOf[F[_, _, _, _, _]]

}
