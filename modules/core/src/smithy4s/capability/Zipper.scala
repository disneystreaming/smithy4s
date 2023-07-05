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

trait Zipper[F[_]] extends Covariant[F] {
  def pure[A](a: A): F[A]

  def zipMapAll[A](seq: IndexedSeq[F[Any]])(f: IndexedSeq[Any] => A): F[A]

  def zipMap[A, B, C](fa: F[A], fb: F[B])(f: (A, B) => C): F[C] =
    zipMapAll(IndexedSeq(fa, fb).asInstanceOf[IndexedSeq[F[Any]]])(seq =>
      f(seq(0).asInstanceOf[A], seq(1).asInstanceOf[B])
    )

  override def map[A, B](fa: F[A])(f: A => B): F[B] =
    zipMapAll(IndexedSeq(fa.asInstanceOf[F[Any]]))(seq =>
      f(seq(0).asInstanceOf[A])
    )

}

object Zipper extends ZipperInstances0 {

  def apply[F[_]](implicit ev: Zipper[F]): ev.type = ev

}

trait ZipperInstances0 {
  implicit def zipperInstanceForEither[E]: Zipper[Either[E, *]] =
    smithy4s.capability.instances.either.zipperInstanceForEither[E]
}
