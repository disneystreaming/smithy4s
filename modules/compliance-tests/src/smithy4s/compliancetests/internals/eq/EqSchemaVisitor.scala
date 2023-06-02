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

package smithy4s.compliancetests.internals.eq

import smithy4s.compliancetests.internals.eq.Smithy4sEqInstances._
import cats.kernel.Eq
import smithy4s._
import smithy4s.schema.{Schema, _}

import java.util.UUID
import smithy4s.capability.EncoderK

object EqSchemaVisitor extends SchemaVisitor[Eq] { self =>
  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): Eq[P] = primitiveEq(tag)

  override def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): Eq[C[A]] = {
    implicit val memberEq: Eq[A] = self(member)
    tag match {
      case CollectionTag.ListTag       => Eq[List[A]]
      case CollectionTag.SetTag        => Eq[Set[A]]
      case CollectionTag.VectorTag     => Eq[Vector[A]]
      case CollectionTag.IndexedSeqTag => Eq[IndexedSeq[A]]
    }
  }

  override def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): Eq[Map[K, V]] = {
    implicit val valueEq: Eq[V] = self(value)
    Eq[Map[K, V]]
  }

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      tag: EnumTag,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): Eq[E] =
    Eq.by { x =>
      val enumX = total(x)
      (enumX.intValue, enumX.stringValue)
    }

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): Eq[S] = { (x: S, y: S) =>
    {
      def forField[A2](field: Field[Schema, S, A2]): Boolean = {
        val eqField = field.foldK(new Field.FolderK[Schema, S, Eq]() {
          override def onRequired[A](
              label: String,
              instance: Schema[A],
              get: S => A
          ): Eq[A] = self(instance)

          override def onOptional[A](
              label: String,
              instance: Schema[A],
              get: S => Option[A]
          ): Eq[Option[A]] = {
            val showA = self(instance)
            (x: Option[A], y: Option[A]) =>
              (x, y) match {
                case (Some(a), Some(b)) => showA.eqv(a, b)
                case (None, None)       => true
                case _                  => false
              }
          }
        })
        eqField.eqv(field.get(x), field.get(y))
      }

      fields.forall(forField(_))

    }

  }

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[U, _]],
      dispatch: Alt.Dispatcher[Schema, U]
  ): Eq[U] = {
    // A version of `Eq` that assumes that the RHS is "up-casted" to U.
    trait AltEq[A] {
      def eqv(a: A, u: U): Boolean
    }

    // The encoded form that Eq works against is a partially-applied curried function.
    implicit val encoderKInstance = new EncoderK[AltEq, U => Boolean] {
      def apply[A](fa: AltEq[A], a: A): U => Boolean = { (u: U) =>
        fa.eqv(a, u)
      }
      def absorb[A](f: A => (U => Boolean)): AltEq[A] = new AltEq[A] {
        def eqv(a: A, u: U): Boolean = f(a)(u)
      }
    }

    val precompiler = new Alt.Precompiler[Schema, AltEq] {
      def apply[A](label: String, instance: Schema[A]): AltEq[A] = {
        // Here we "cheat" to recover the `Alt` corresponding to `A`, as this information
        // is lost in the precompiler.
        val altA =
          alternatives.find(_.label == label).get.asInstanceOf[SchemaAlt[U, A]]
        // We're using it to get a function that lets us project the `U` against `A`.
        // `U` is not necessarily an `A, so this function returns an `Option`
        val projectA: U => Option[A] = dispatch.projector(altA)
        val eqA = instance.compile(self)
        new AltEq[A] {
          def eqv(a: A, u: U): Boolean = projectA(u) match {
            case None => false // U is not an A.
            case Some(a2) =>
              eqA.eqv(a, a2) // U is an A, we delegate the comparison
          }
        }
      }
    }

    val altEqU = dispatch.compile(precompiler)
    new Eq[U] {
      def eqv(x: U, y: U): Boolean = altEqU.eqv(x, y)
    }
  }

  override def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): Eq[B] = {
    val aEq: Eq[A] = self(schema)
    (x: B, y: B) => aEq.eqv(bijection.from(x), bijection.from(y))
  }

  override def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): Eq[B] = {
    val eqA: Eq[A] = self(schema)
    (x: B, y: B) => eqA.eqv(refinement.from(x), refinement.from(y))
  }

  override def lazily[A](
      shapeId: ShapeId,
      hints: Hints,
      suspend: Lazy[Schema[A]]
  ): Eq[A] = {
    val eq = suspend.map(self(_))
    (x: A, y: A) => eq.value.eqv(x, y)
  }

  def primitiveEq[P](primitive: Primitive[P]): Eq[P] = {
    primitive match {
      case Primitive.PShort      => Eq[Short]
      case Primitive.PInt        => Eq[Int]
      case Primitive.PFloat      => floatEq
      case Primitive.PLong       => Eq[Long]
      case Primitive.PDouble     => doubleEq
      case Primitive.PBigInt     => Eq[BigInt]
      case Primitive.PBigDecimal => Eq[BigDecimal]
      case Primitive.PBoolean    => Eq[Boolean]
      case Primitive.PString     => Eq[String]
      case Primitive.PUUID       => Eq[UUID]
      case Primitive.PByte       => Eq[Byte]
      case Primitive.PBlob       => Eq[ByteArray]
      case Primitive.PDocument   => Eq[Document]
      case Primitive.PTimestamp  => Eq[Timestamp]
      case Primitive.PUnit       => Eq[Unit]
    }
  }

}
