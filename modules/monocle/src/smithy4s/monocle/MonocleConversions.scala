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

package smithy4s.monocle

object MonocleConversions {

  implicit def smithy4sToMonocleLens[S, A](
      smithy4sLens: smithy4s.optics.Lens[S, A]
  ): monocle.Lens[S, A] =
    monocle.Lens[S, A](smithy4sLens.get)(smithy4sLens.replace(_))

  implicit def smithy4sToMonoclePrism[S, A](
      smithy4sPrism: smithy4s.optics.Prism[S, A]
  ): monocle.Prism[S, A] =
    monocle.Prism(smithy4sPrism.getOption)(smithy4sPrism.project)

  implicit def smithy4sToMonocleOptional[S, A](
      smithy4sOptional: smithy4s.optics.Optional[S, A]
  ): monocle.Optional[S, A] =
    monocle.Optional(smithy4sOptional.getOption)(smithy4sOptional.replace)

}
