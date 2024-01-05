/*
 *  Copyright 2021-2024 Disney Streaming
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

import scala.util.control.NonFatal

/**
  * A surjection of a partial function A => Either[String, B] and a total function B => A.
  *
  * A surjection MUST abide by the round-tripping property, namely, for all input A that passes the
  * validation function
  *
  * surjection(input).map(surjection.from) == Right(input)
  */
trait Surjection[A, B] extends Function[A, Either[String, B]] { outer =>
  def to(a: A): Either[String, B]
  def from(b: B): A

  final def apply(a: A): Either[String, B] = to(a)

  final def imapFull[A0, B0](
      sourceBijection: Bijection[A, A0],
      targetBijection: Bijection[B, B0]
  ): Surjection[A0, B0] = new Surjection[A0, B0] {
    def to(a0: A0): Either[String, B0] =
      outer.to(sourceBijection.from(a0)).map(targetBijection)
    def from(b0: B0): A0 = sourceBijection(outer.from(targetBijection.from(b0)))
  }

  final def imapSource[A0](bijection: Bijection[A, A0]): Surjection[A0, B] =
    imapFull(bijection, Bijection.identity)

  final def imapTarget[B0](bijection: Bijection[B, B0]): Surjection[A, B0] =
    imapFull(Bijection.identity, bijection)
}

object Surjection {
  def apply[A, B](to: A => Either[String, B], from: B => A): Surjection[A, B] =
    new Impl[A, B](to, from)

  def catching[A, B](to: A => B, from: B => A): Surjection[A, B] =
    new Impl(
      a =>
        try { Right(to(a)) }
        catch { case NonFatal(e) => Left(e.getMessage) },
      from
    )

  private class Impl[A, B](
      toFunction: A => Either[String, B],
      fromFunction: B => A
  ) extends Surjection[A, B] {
    def to(a: A): Either[String, B] = toFunction(a)
    def from(b: B): A = fromFunction(b)
  }
}
