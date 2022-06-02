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

package smithy4s.capability

import smithy4s.ConstraintError

trait Covariant[F[_]] {
  def map[A, B](fa: F[A])(f: A => B): F[B]
  def emap[A, B](fa: F[A])(f: A => Either[ConstraintError, B]): F[B]
}

object Covariant {

  def apply[F[_]](implicit instance: Covariant[F]): Covariant[F] = instance

}
