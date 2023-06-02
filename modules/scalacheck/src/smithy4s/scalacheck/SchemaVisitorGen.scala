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

package smithy4s
package scalacheck

import smithy4s.schema._

import org.scalacheck.Gen

import scala.jdk.CollectionConverters._
import smithy4s.schema.Primitive._
import smithy.api.Length

object SchemaVisitorGen extends SchemaVisitorGen

abstract class SchemaVisitorGen extends SchemaVisitor[Gen] { self =>

  def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): Gen[P] = {
    tag match {
      case PShort  => chooseNumAux(hints, Short.MinValue, Short.MaxValue)
      case PDouble => chooseNumAux(hints, Double.MinValue, Double.MaxValue)
      case PInt    => chooseNumAux(hints, Int.MinValue, Int.MaxValue)
      case PLong   => chooseNumAux(hints, Long.MinValue, Long.MaxValue)
      case PFloat  => chooseNumAux(hints, Float.MinValue, Float.MaxValue)
      case PBigDecimal =>
        chooseNumAux(hints, Long.MinValue, Long.MaxValue).map(BigDecimal.apply)
      case PBigInt =>
        chooseNumAux(hints, Long.MinValue, Long.MaxValue).map(BigInt.apply)
      case PUnit     => ()
      case PUUID     => Gen.uuid
      case PByte     => Gen.oneOf(Range(1, 0xff)).map(_.toByte)
      case PDocument => Smithy4sGen.genDocument(1)
      case PBoolean  => Gen.oneOf(true, false)
      case PString =>
        length(hints).flatMap(l => Gen.stringOfN(l, Gen.asciiPrintableChar))
      case PTimestamp => Smithy4sGen.genTimestamp
      case PBlob =>
        length(hints)
          .flatMap(l => Gen.stringOfN(l, Gen.asciiPrintableChar))
          .map(_.getBytes)
          .map(ByteArray.apply)
    }
  }

  def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): Gen[C[A]] =
    length(hints)
      .flatMap(l => Gen.listOfN(l, member.compile(this)))
      .map(l => tag.fromIterator(l.iterator))

  def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): Gen[Map[K, V]] =
    length(hints).flatMap(l =>
      Gen.mapOfN(
        l,
        key.compile(this).flatMap(k => value.compile(this).map(k -> _))
      )
    )
  def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      tag: EnumTag,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): Gen[E] =
    Gen.oneOf(values.map(_.value))
  def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): Gen[S] =
    Gen.sequence(fields.map(f => genField(f))).flatMap { arrayList =>
      make(arrayList.asScala.toVector)
    }
  def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[U, _]],
      dispatch: Alt.Dispatcher[Schema, U]
  ): Gen[U] = {
    if (alternatives.size == 1) genAlt(alternatives(0))
    else
      Gen.oneOf(
        genAlt(alternatives(0)),
        genAlt(alternatives(1)),
        alternatives.drop(2).map(a => genAlt(a)): _*
      )
  }

  def biject[A, B](schema: Schema[A], bijection: Bijection[A, B]): Gen[B] =
    schema.compile(this).map(bijection)

  def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): Gen[B] = schema.compile(this).map(refinement.unsafe)
  def lazily[A](shapeId: ShapeId, hints: Hints, suspend: Lazy[Schema[A]]): Gen[A] =
    Gen.lzy(suspend.map(_.compile(this)).value)

  // //////////////////////////////////////////////////////////////////////////////////////
  // // HELPER FUNCTIONS
  // //////////////////////////////////////////////////////////////////////////////////////

  private def genAlt[S, A](alt: Alt[Schema, S, A]): Gen[S] =
    alt.instance.compile(this).map(alt.inject)

  private def genField[S, A](field: Field[Schema, S, A]): Gen[A] =
    Gen.lzy(field.mapK(this).instanceA {
      new Field.ToOptional[Gen] {
        def apply[AA](genA: Gen[AA]): Gen[Option[AA]] = Gen.option(genA)
      }
    })

  private def bigDecimalToInt(b: BigDecimal): Int = if (
    b < BigDecimal(Int.MinValue)
  ) Int.MinValue
  else if (b > BigDecimal(Int.MaxValue)) Int.MaxValue
  else b.toInt

  private def chooseNumAux[T](hints: Hints, minT: T, maxT: T)(implicit
      num: Numeric[T],
      c: Gen.Choose[T]
  ) = hints.get[smithy.api.Range] match {
    case None => Gen.chooseNum[T](minT, maxT)
    case Some(range) =>
      val min = range.min.map(bigDecimalToInt).map(num.fromInt).getOrElse(minT)
      val max = range.max.map(bigDecimalToInt).map(num.fromInt).getOrElse(maxT)
      Gen.chooseNum[T](min, max)
  }

  private def length(hints: Hints): Gen[Int] = hints.get[Length] match {
    case None => Gen.const(5)
    case Some(length) =>
      val min = length.min.map(_.toInt).getOrElse(0)
      val max = length.max.map(_.toInt).getOrElse(min + 5)
      Gen.chooseNum[Int](min, max)
  }

}
