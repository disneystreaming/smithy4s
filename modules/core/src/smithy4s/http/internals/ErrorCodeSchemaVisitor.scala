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

private[smithy4s] object ErrorCodeSchemaVisitor
    extends SchemaVisitor.Default[HttpCode] {
  def default[A]: A => Option[Int] = _ => None

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[U, _]],
      dispatcher: Alt.Dispatcher[Schema, U]
  ): HttpCode[U] = { (s) =>
    processAltWithValue(dispatcher.underlying(s))
  }

  def processAltWithValue[S, B](
      withValue: Alt.WithValue[Schema, S, B]
  ): Option[Int] = {
    val httpCode = apply(withValue.alt.instance)
    httpCode(withValue.value)
  }

  override def lazily[A](suspend: Lazy[Schema[A]]): HttpCode[A] =
    apply(suspend.value)

  override def biject[A, B](
      schema: Schema[A],
      to: A => B,
      from: B => A
  ): HttpCode[B] = {
    val httpCode = apply(schema)
    b => httpCode(from(b))
  }

  override def surject[A, B](
      schema: Schema[A],
      to: Refinement[A, B],
      from: B => A
  ): HttpCode[B] = {
    val httpCode = apply(schema)
    b => httpCode(from(b))
  }

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): HttpCode[S] = { _ =>
    hints
      .get(smithy.api.HttpError)
      .map(_.value)
      .orElse(hints.get(smithy.api.Error).map {
        case smithy.api.Error.CLIENT => 400
        case smithy.api.Error.SERVER => 500
      })
  }

}
