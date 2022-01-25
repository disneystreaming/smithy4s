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

import schematic.OneOf
import schematic.OptionalField
import schematic.RequiredField

package object syntax
    extends schematic.enumeration.Syntax
    with schematic.list.ClosedSyntax[Schematic]
    with schematic.vector.ClosedSyntax[Schematic]
    with schematic.set.ClosedSyntax[Schematic]
    with schematic.map.ClosedSyntax[Schematic]
    with schematic.union.ClosedSyntax[Schematic]
    with schematic.struct.ClosedSyntax[Schematic]
    with schematic.suspended.ClosedSyntax[Schematic]
    with schematic.bijection.ClosedSyntax[Schematic]
    with Hints.ClosedSyntax[Schematic]
    with errorUnion.Syntax {

  val short: Schema[Short] =
    schematic.short.Schema.withHints(ShapeId("smithy.api", "Short"))

  val int: Schema[Int] =
    schematic.int.Schema.withHints(ShapeId("smithy.api", "Integer"))

  val long: Schema[Long] =
    schematic.long.Schema.withHints(ShapeId("smithy.api", "Long"))

  val double: Schema[Double] =
    schematic.double.Schema.withHints(ShapeId("smithy.api", "BigDecimal"))

  val float: Schema[Float] =
    schematic.float.Schema.withHints(ShapeId("smithy.api", "Float"))

  val bigint: Schema[BigInt] =
    schematic.bigint.Schema.withHints(ShapeId("smithy.api", "BigInteger"))

  val bigdecimal: Schema[BigDecimal] =
    schematic.bigdecimal.Schema.withHints(ShapeId("smithy.api", "BigDecimal"))

  val string: Schema[String] =
    schematic.string.Schema.withHints(ShapeId("smithy.api", "String"))

  val boolean: Schema[Boolean] =
    schematic.boolean.Schema.withHints(ShapeId("smithy.api", "Boolean"))

  val byte: Schema[Byte] =
    schematic.byte.Schema.withHints(ShapeId("smithy.api", "Byte"))

  val bytes: Schema[schematic.ByteArray] =
    schematic.bytes.Schema.withHints(ShapeId("smithy.api", "Blob"))

  val unit: Schema[Unit] =
    schematic.unit.Schema.withHints(ShapeId("smithy.api", "Unit"))

  val timestamp: Schema[Timestamp] =
    Timestamp.Schema.withHints(ShapeId("smithy.api", "Timestamp"))

  val document: Schema[Document] =
    Document.Schema.withHints(ShapeId("smithy.api", "Document"))

  val uuid: Schema[java.util.UUID] =
    schematic.uuid.Schema.withHints(ShapeId("smithy4s.api", "UUID"))

  def constant[A](make: => A): Schema[A] =
    new schematic.struct.Schema[Schematic, A](Vector.empty, _ => make)

  implicit class withHintsSyntax[A](val schema: Schema[A]) extends AnyVal {
    def withHints(hints: Hints): Schema[A] = schema match {
      case s: Hints.Schema[_, _] =>
        s.asInstanceOf[Hints.Schema[Schematic, A]].addHints(hints)
      case other => new Hints.Schema[Schematic, A](other, hints)
    }

    def withHints(hints: Hint*): Schema[A] = withHints(Hints(hints: _*))
  }

  implicit class withHintsFieldSyntax[Z, A](
      val field: schematic.StructureField[Schematic, Z, A]
  ) extends AnyVal {
    def withHints(hints: Hint*): schematic.StructureField[Schematic, Z, A] =
      field match {
        case RequiredField(label, schema, get) =>
          RequiredField(label, schema.withHints(hints: _*), get)
        case opt => // Scala 2 is unable to reconcile HK GADT
          val foo = opt.asInstanceOf[OptionalField[Schematic, Z, Any]]
          OptionalField(opt.label, foo.schema.withHints(hints: _*), foo.get)
            .asInstanceOf[schematic.StructureField[Schematic, Z, A]]
      }
  }

  implicit class withHintsOneOfSyntax[Z, A](
      val oneOf: schematic.OneOf[Schematic, Z, A]
  ) extends AnyVal {
    def withHints(hints: Hint*): schematic.OneOf[Schematic, Z, A] =
      OneOf(oneOf.label, oneOf.schema.withHints(hints: _*), oneOf.inject)
  }

}
