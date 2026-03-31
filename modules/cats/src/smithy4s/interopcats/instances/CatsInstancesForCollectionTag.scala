/*
 *  Copyright 2021-2025 Disney Streaming
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

package smithy4s.interopcats.instances

import cats._
import cats.data._
import cats.syntax.all._
import cats.kernel.instances.StaticMethods.wrapMutableIndexedSeq
import smithy4s.schema.CollectionTag

private[interopcats] class CatsInstancesForCollectionTag[
    C[_]
] private[interopcats] (implicit CT: CollectionTag[C])
    extends Traverse[C]
    with Alternative[C]
    with Monad[C]
    with CoflatMap[C]
    with Align[C] {

  def pure[A](a: A): C[A] = CT.build(_(a))

  override def flatMap[A, B](fa: C[A])(f: A => C[B]): C[B] =
    CT.fromIterator {
      CT.iterator(fa).flatMap { a =>
        CT.iterator(f(a))
      }
    }

  override def tailRecM[A, B](a: A)(f: A => C[Either[A, B]]): C[B] =
    CT.build { (put: B => Unit) =>
      @annotation.tailrec
      def go(lists: List[Iterator[Either[A, B]]]): Unit =
        lists match {
          case hd :: tail if hd.isEmpty =>
            go(tail)

          case hd :: tail =>
            hd.next() match {
              case Right(b) =>
                put(b)
                go(hd :: tail)
              case Left(a) =>
                go(CT.iterator(f(a)) :: hd :: tail)
            }

          case Nil => ()
        }

      go(CT.iterator(f(a)) :: Nil)
    }

  override def traverse[G[_]: Applicative, A, B](
      fa: C[A]
  )(f: A => G[B]): G[C[B]] =
    if (CT.isEmpty(fa)) CT.empty.pure[G]
    else
      Chain
        .traverseViaChain {
          val as = collection.mutable.ArrayBuffer[A]()
          as ++= CT.iterator(fa)
          wrapMutableIndexedSeq(as)
        }(f)
        .map(c => CT.fromIterator(c.iterator))

  override def foldLeft[A, B](fa: C[A], b: B)(f: (B, A) => B): B =
    CT.iterator(fa).foldLeft(b)(f)

  override def foldRight[A, B](fa: C[A], lb: Eval[B])(
      f: (A, Eval[B]) => Eval[B]
  ): Eval[B] = {
    def loop(as: Iterator[A]): Eval[B] =
      if (as.hasNext) f(as.next(), Eval.defer(loop(as)))
      else lb

    Eval.defer(loop(CT.iterator(fa)))
  }

  override def empty[A]: C[A] = CT.empty

  override def combineK[A](x: C[A], y: C[A]): C[A] =
    CT.fromIterator(CT.iterator(x) ++ CT.iterator(y))

  override def coflatMap[A, B](fa: C[A])(f: C[A] => B): C[B] = CT.build {
    (put: B => Unit) =>
      CT.iterator(fa)
        .foreach(a => put(f(CT.build(_(a)))))
  }

  override def functor: Functor[C] = this

  override def align[A, B](fa: C[A], fb: C[B]): C[Ior[A, B]] = CT.build {
    (put: Ior[A, B] => Unit) =>
      @annotation.tailrec
      def loop(as: Iterator[A], bs: Iterator[B]): Unit =
        if (as.hasNext && bs.hasNext) {
          put(Ior.Both(as.next(), bs.next()))
          loop(as, bs)
        } else if (as.hasNext) as.foreach(a => put(Ior.left(a)))
        else if (bs.hasNext) bs.foreach(b => put(Ior.right(b)))
        else ()

      loop(CT.iterator(fa), CT.iterator(fb))
  }
}
