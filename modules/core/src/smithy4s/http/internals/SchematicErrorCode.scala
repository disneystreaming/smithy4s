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
package http
package internals

import smithy4s.schema._

private[smithy4s] object SchematicErrorCode extends StubSchematic[HttpCode] {

  def default[A]: (A, Hints) => Option[Int] = (_, _) => None

  override def union[S](
      first: Alt[HttpCode, S, _],
      rest: Vector[Alt[HttpCode, S, _]]
  )(total: S => Alt.WithValue[HttpCode, S, _]): HttpCode[S] = { (s, hints) =>
    processAltWithValue(total(s), hints)
  }

  def processAltWithValue[S, B](
      withValue: Alt.WithValue[HttpCode, S, B],
      hints: Hints
  ): Option[Int] =
    withValue.alt.instance(withValue.value, hints)

  override def suspend[A](f: Lazy[HttpCode[A]]): HttpCode[A] = f.value

  override def bijection[A, B](
      f: HttpCode[A],
      to: A => B,
      from: B => A
  ): HttpCode[B] =
    (b, hints) => f(from(b), hints)

  override def surjection[A, B](
      f: HttpCode[A],
      to: Refinement[A, B],
      from: B => A
  ): HttpCode[B] =
    (b, hints) => f(from(b), hints)

  override def struct[S](
      fields: Vector[Field[HttpCode, S, _]]
  )(f: Vector[Any] => S): HttpCode[S] = (_, hints) =>
    hints
      .get(smithy.api.HttpError)
      .map(_.value)
      .orElse(hints.get(smithy.api.Error).map {
        case smithy.api.Error.CLIENT => 400
        case smithy.api.Error.SERVER => 500
      })

  override def withHints[A](fa: HttpCode[A], hints: Hints): HttpCode[A] =
    (a: A, _: Hints) => fa(a, hints)

}
