/*
 *  Copyright 2021 Disney Streaming
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

package schematic

/**
  * Provides functions to model coproducts (sealed-hints).
  */
object union {

  trait Schematic[F[_]] {

    /**
      * Models a coproduct as a list of alternatives. Typesafety is guaranteed by the
      * `total` function, which ensures that any value of the supertype can be downcasted
      * to an alternative.
      */
    def union[S](first: Alt[F, S, _], rest: Vector[Alt[F, S, _]])(
        total: S => Alt.WithValue[F, S, _]
    ): F[S]

  }

  class Schema[S[x[_]] <: Schematic[x], U](
      first: OneOf[S, U, _],
      rest: Vector[OneOf[S, U, _]],
      total: U => OneOf.WithValue[S, U, _]
  ) extends schematic.Schema[S, U] {

    def cast[A](oneOf: OneOf[S, U, A]): OneOf[S, U, Any] =
      oneOf.asInstanceOf[OneOf[S, U, Any]]

    val cf = cast(first)
    val cr = rest.asInstanceOf[Vector[OneOf[S, U, Any]]]
    def compile[F[_]](s: S[F]): F[U] = {
      // Memoising the compilation to avoid recompiling dynamically.
      // This implies that the developer should ensure that the values
      // used in to populate the `total` function are assigned to vals
      // as opposed to defs, and are present in the first/rest list.
      // Unfortunately Scala doesn't allow for much safety there, without
      // relying on shapeless or other invasive mechanisms.
      val memoised: Map[OneOf[S, U, Any], Alt[F, U, Any]] = {
        ((cf -> cf.compile(s)) +: cr.map(r => r -> r.compile(s))).toMap
      }
      s.union(memoised(cf), cr.map(o => memoised(o)))(union => {
        val withValue = total(union).asInstanceOf[OneOf.WithValue[S, U, Any]]
        memoised(withValue.alt)(withValue.value)
      })
    }

  }

  trait OpenSyntax {
    def union[S[x[_]] <: Schematic[x], U](
        first: OneOf[S, U, _],
        rest: OneOf[S, U, _]*
    )(total: U => OneOf.WithValue[S, U, _]): schematic.Schema[S, U] =
      new Schema(first, rest.toVector, total)

  }

  trait ClosedSyntax[S[x[_]] <: Schematic[x]] {
    def union[U](first: OneOf[S, U, _], rest: OneOf[S, U, _]*)(
        total: U => OneOf.WithValue[S, U, _]
    ): schematic.Schema[S, U] =
      new Schema(first, rest.toVector, total)
  }

}
