/*
 *  Copyright 2021-2026 Disney Streaming
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

package smithy4s.interopchimney

import io.scalaland.chimney.PartialTransformer
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.partial.Result
import smithy4s.Bijection
import smithy4s.Surjection

package object instances {
  implicit def bijectionToTransformer[A, B](implicit
      bijection: Bijection[A, B]
  ): Transformer[A, B] = bijection.to

  implicit def bijectionFromTransformer[A, B](implicit
      bijection: Bijection[A, B]
  ): Transformer[B, A] = bijection.from

  implicit def surjectionToPartialTransformer[A, B](implicit
      surjection: Surjection[A, B]
  ): PartialTransformer[A, B] =
    PartialTransformer(a => Result.fromEitherString(surjection.to(a)))

  implicit def surjectionFromTransformer[A, B](implicit
      surjection: Surjection[A, B]
  ): Transformer[B, A] = surjection.from
}
