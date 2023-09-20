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

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

/**
  * A zipper is similar in notion to `Applicative`, in that it allows for
  * combining several values of `F` in non-sequential ways.
  *
  * The encoding here is specialised towards the traversal of sequences,
  * which is something that's done heavily in this codebase.
  */
trait Zipper[F[_]] {
  def pure[A](a: A): F[A]

  def zipMapAll[A](seq: IndexedSeq[F[Any]])(f: IndexedSeq[Any] => A): F[A]

  def zipMap[A, B, C](fa: F[A], fb: F[B])(f: (A, B) => C): F[C] =
    zipMapAll(IndexedSeq(fa, fb).asInstanceOf[IndexedSeq[F[Any]]])(seq =>
      f(seq(0).asInstanceOf[A], seq(1).asInstanceOf[B])
    )

}

object Zipper {

  def apply[F[_]](implicit ev: Zipper[F]): ev.type = ev

  implicit def eitherZipper[E]: Zipper[Either[E, *]] =
    new Zipper[Either[E, *]] {
      def pure[A](a: A): Either[E, A] = Right(a)

      override def zipMap[A, B, C](fa: Either[E, A], fb: Either[E, B])(
          f: (A, B) => C
      ): Either[E, C] = (fa, fb) match {
        case (l @ Left(_), _)     => l.asInstanceOf[Either[E, C]]
        case (_, r @ Left(_))     => r.asInstanceOf[Either[E, C]]
        case (Right(a), Right(b)) => Right(f(a, b))
      }

      override def zipMapAll[A](
          seq: IndexedSeq[Either[E, Any]]
      )(f: IndexedSeq[Any] => A): Either[E, A] = {
        val builder = IndexedSeq.newBuilder[Any]
        var i = 0
        var error: Left[E, Any] = null
        while (error == null && i < seq.size) {
          seq(i) match {
            case l @ Left(_) => error = l.asInstanceOf[Left[E, Any]]
            case Right(r)    => builder += r
          }
          i += 1
        }
        if (error != null) error.asInstanceOf[Left[E, A]]
        else Right(f(builder.result()))
      }
    }

  implicit def futureZipper(implicit ec: ExecutionContext): Zipper[Future] =
    new Zipper[Future] {
      override def zipMapAll[A](seq: IndexedSeq[Future[Any]])(
          f: IndexedSeq[Any] => A
      ): Future[A] = Future.sequence(seq).map(f)
      def pure[A](a: A): Future[A] = Future.successful(a)
    }
}
