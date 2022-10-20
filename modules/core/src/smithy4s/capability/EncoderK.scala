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

/**
  * A typeclass abstracting over the notion of encoder.
  *
  * Useful in particular when encoding unions
  */
trait EncoderK[F[_], Result] extends Contravariant[F] {
  def apply[A](fa: F[A], a: A): Result
  def absorb[A](f: A => Result): F[A]
  def contramap[A, B](fa: F[A])(f: B => A): F[B] =
    absorb[B](b => apply[A](fa, f(b)))
}

object EncoderK {
  implicit def encoderKForFunction[B]: EncoderK[* => B, B] =
    new EncoderK[* => B, B] {
      def apply[A](fa: A => B, a: A): B = fa(a)
      def absorb[A](f: A => B): A => B = f
    }

}
