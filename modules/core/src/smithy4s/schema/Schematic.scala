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
package schema

import smithy4s.schema.Schema._

trait Schematic[F[_]] {

  // numeric
  def short: F[Short]
  def int: F[Int]
  def float: F[Float]
  def long: F[Long]
  def double: F[Double]
  def bigint: F[BigInt]
  def bigdecimal: F[BigDecimal]

  // misc primitives
  def boolean: F[Boolean]
  def bytes: F[ByteArray]
  def uuid: F[java.util.UUID]
  def byte: F[Byte]
  def string: F[String]
  def document: F[Document]
  def unit: F[Unit]
  def timestamp: F[Timestamp]

  // collections
  def set[S](fs: F[S]): F[Set[S]]
  def list[S](fs: F[S]): F[List[S]]
  def map[K, V](fk: F[K], fv: F[V]): F[Map[K, V]]

  // Other
  def suspend[A](f: Lazy[F[A]]): F[A]
  def bijection[A, B](f: F[A], to: A => B, from: B => A): F[B]
  def surjection[A, B](
      f: F[A],
      to: Refinement[A, B],
      from: B => A
  ): F[B]
  def withHints[A](fa: F[A], hints: Hints): F[A]
  def enumeration[A](
      to: A => (String, Int),
      fromName: Map[String, A],
      fromOrdinal: Map[Int, A]
  ): F[A]

  def struct[S](fields: Vector[Field[F, S, _]])(const: Vector[Any] => S): F[S]

  def union[S](
      first: Alt[F, S, _],
      rest: Vector[Alt[F, S, _]]
  )(total: S => Alt.WithValue[F, S, _]): F[S]
}

object Schematic {

  def toPolyFunction[F[_]](schematic: Schematic[F]): Schema ~> F =
    new (Schema ~> F) { self =>
      import schematic._
      def apply[A](fa: Schema[A]): F[A] = {
        val rawCodec: F[A] = fa match {
          case PrimitiveSchema(_, _, tag) => primitive(tag)
          case EnumerationSchema(_, _, values, total) =>
            val to: A => (String, Int) = a => {
              val t = total(a)
              (t.stringValue, t.ordinal)
            }
            val fromOrdinal = values.map { v => v.ordinal -> v.value }.toMap
            val fromName = values.map { v => v.stringValue -> v.value }.toMap
            enumeration(to, fromName, fromOrdinal)
          case SetSchema(_, _, member) =>
            set(apply(member))
          case ListSchema(_, _, member) =>
            list(apply(member))
          case MapSchema(_, _, key, value) =>
            map(apply(key), apply(value))
          case BijectionSchema(underlying, to, from) =>
            bijection(apply(underlying), to, from)
          case SurjectionSchema(underlying, to, from) =>
            surjection(apply(underlying), to, from)
          case StructSchema(_, _, fields, make) =>
            struct(fields.map(Field.shiftHintsK(_)).map(_.mapK(self)))(make)
          case schema @ Schema.UnionSchema(_, _, _, _) => {
            compileUnion(schema)
          }
          case LazySchema(suspendedSchema) =>
            suspend(suspendedSchema.map(self(_)))
        }
        withHints(rawCodec, fa.hints)
      }

      import Primitive._
      def primitive[A](p: Primitive[A]): F[A] = p match {
        case PShort      => short
        case PInt        => int
        case PFloat      => float
        case PLong       => long
        case PDouble     => double
        case PBigInt     => bigint
        case PBigDecimal => bigdecimal
        case PBoolean    => boolean
        case PString     => string
        case PUUID       => uuid
        case PByte       => byte
        case PBlob       => bytes
        case PDocument   => document
        case PTimestamp  => timestamp
        case PUnit       => unit
      }

      def compileUnion[U](schema: UnionSchema[U]): F[U] = {
        val alts: Vector[SchemaAlt[U, _]] = schema.alternatives
        val head = alts.head
        val tail = alts.tail
        // Pre-compiles the schemas associated to each alternative. This is important
        // because we need to avoid compiling the schemas to codecs upon every dispatch
        val precompiledAlts =
          (Alt.shiftHintsK[U] andThen Alt.liftK[Schema, F, U](self))
            .unsafeCache(alts.map(smithy4s.Existential.wrap(_)))

        def processAlt[A](
            altWithValue: Alt.WithValue[Schema, U, A]
        ): Alt.WithValue[F, U, A] = {
          val Alt.WithValue(alt, value) = altWithValue
          val altF: Alt[F, U, A] = precompiledAlts(alt)
          Alt.WithValue(altF, value)
        }
        val dispatch: U => Alt.WithValue[F, U, _] = u =>
          processAlt(schema.dispatch(u))
        union(precompiledAlts(head), tail.map(precompiledAlts(_)))(dispatch)
      }
    }

}
