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

package smithy4s.schema

import smithy4s.ShapeTag

/**
  * An ErrorSchema is similar to a UnionSchema in that it exposes `alternatives` and `ordinal` values,
  * and therefore can be manipulated similarly to UnionSchemas.
  *
  * Additionally, it carries functions to go from E to Throwable and vice-versa. This is used by
  * interpreters to inject data into error channels of effect types, or to recover data from
  * an error-channel prior to serialisation.
  */
case class ErrorSchema[E] private[smithy4s] (
    schema: Schema[E],
    liftError: Throwable => Option[E],
    unliftError: E => Throwable
) {

  final def mapSchema(f: Schema[E] => Schema[E]): ErrorSchema[E] =
    copy(schema = f(schema))

  final val ordinal: E => Int = schema match {
    case u: Schema.UnionSchema[E] => u.ordinal
    case _                        => (_: E) => 0
  }

  final val alternatives: Vector[smithy4s.schema.Alt[E, _]] = schema match {
    case u: Schema.UnionSchema[E] => u.alternatives
    case other =>
      Vector(
        smithy4s.schema
          .Alt[E, E](
            other.shapeId.name,
            other,
            identity[E],
            { case e => e }
          )
      )
  }
}

object ErrorSchema {

  trait Companion[E] extends ShapeTag.Companion[E] {
    def liftError(throwable: Throwable): Option[E]
    def unliftError(e: E): Throwable
    def errorSchema: ErrorSchema[E] =
      ErrorSchema(schema, liftError, unliftError)
  }

}
