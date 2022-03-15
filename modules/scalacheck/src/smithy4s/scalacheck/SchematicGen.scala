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

import java.{util => ju}
import scala.jdk.CollectionConverters._

trait SchematicGen extends Schematic[Gen] {

  def unit: Gen[Unit] = Gen.const(())

  def short: Gen[Short] =
    Gen.chooseNum(Short.MinValue, Short.MaxValue)

  def int: Gen[Int] =
    Gen.chooseNum(Int.MinValue, Int.MaxValue)

  def long: Gen[Long] =
    LongGen.gen()

  def double: Gen[Double] =
    Gen.chooseNum(Double.MinValue, Double.MaxValue)

  def float: Gen[Float] =
    Gen.choose(Float.MinValue, Float.MaxValue)

  def string: Gen[String] =
    Gen.asciiPrintableStr

  def boolean: Gen[Boolean] =
    Gen.oneOf(true, false)

  def bigint: Gen[BigInt] =
    Gen.chooseNum(Long.MinValue, Long.MaxValue).map(BigInt.apply)

  def bigdecimal: Gen[BigDecimal] =
    Gen.chooseNum(Double.MinValue, Double.MaxValue).map(BigDecimal.apply)

  def byte: Gen[Byte] =
    Gen.oneOf(Range(1, 0xff)).map(_.toByte)

  def bytes: Gen[ByteArray] =
    Gen.asciiStr.map(_.getBytes).map(ByteArray.apply)

  def uuid: Gen[ju.UUID] = Gen.uuid

  def list[S](fs: Gen[S]): Gen[List[S]] =
    Gen.listOf(fs)

  def set[S](fs: Gen[S]): Gen[Set[S]] =
    Gen.listOfN(5, fs).map(_.toSet)

  def vector[S](fs: Gen[S]): Gen[Vector[S]] =
    Gen.listOfN(5, fs).map(_.toVector)

  def map[K, V](fk: Gen[K], fv: Gen[V]): Gen[Map[K, V]] =
    Gen.mapOfN(5, fk.flatMap(k => fv.map(k -> _)))

  def enumeration[A](
      to: A => (String, Int),
      fromString: Map[String, A],
      fromOrdinal: Map[Int, A]
  ): Gen[A] = Gen.oneOf(fromString.values)

  def struct[S](
      fields: Vector[Field[Gen, S, _]]
  )(const: Vector[Any] => S): Gen[S] = {
    Gen.sequence(fields.map(f => genField(f))).flatMap { arrayList =>
      const(arrayList.asScala.toVector)
    }
  }

  private def genField[S, A](field: Field[Gen, S, A]): Gen[A] =
    Gen.lzy(field.instanceA {
      new Field.ToOptional[Gen] {
        def apply[AA](genA: Gen[AA]): Gen[Option[AA]] = Gen.option(genA)
      }
    })

  private def genAlt[S, A](alt: Alt[Gen, S, A]): Gen[S] =
    alt.instance.map(alt.inject)

  def union[S](first: Alt[Gen, S, _], rest: Vector[Alt[Gen, S, _]])(
      total: S => Alt.WithValue[Gen, S, _]
  ): Gen[S] = {
    if (rest.isEmpty) genAlt(first)
    else
      Gen.oneOf(
        genAlt(first),
        genAlt(rest.head),
        rest.tail.map(a => genAlt(a)): _*
      )
  }

  def suspend[A](f: Lazy[Gen[A]]): Gen[A] = Gen.lzy(f.value)

  def bijection[A, B](f: Gen[A], to: A => B, from: B => A): Gen[B] = f.map(to)

  def withHints[A](fa: Gen[A], hints: Hints): Gen[A] = fa
  def timestamp: Gen[Timestamp] = Smithy4sGen.genTimestamp
  def document: Gen[Document] = Smithy4sGen.genDocument(1)

}
