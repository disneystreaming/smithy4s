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
    extends schematic.short.Syntax
    with schematic.int.Syntax
    with schematic.long.Syntax
    with schematic.double.Syntax
    with schematic.float.Syntax
    with schematic.bigint.Syntax
    with schematic.bigdecimal.Syntax
    with schematic.string.Syntax
    with schematic.boolean.Syntax
    with schematic.uuid.Syntax
    with schematic.byte.Syntax
    with schematic.bytes.Syntax
    with schematic.unit.Syntax
    with schematic.enumeration.Syntax
    with Timestamp.Syntax
    with schematic.list.ClosedSyntax[Schematic]
    with schematic.vector.ClosedSyntax[Schematic]
    with schematic.set.ClosedSyntax[Schematic]
    with schematic.map.ClosedSyntax[Schematic]
    with schematic.union.ClosedSyntax[Schematic]
    with schematic.struct.ClosedSyntax[Schematic]
    with schematic.suspended.ClosedSyntax[Schematic]
    with schematic.bijection.ClosedSyntax[Schematic]
    with Hints.ClosedSyntax[Schematic]
    with Document.ClosedSyntax[Schematic]
    with errorUnion.Syntax {

  def constant[A](make: => A): Schema[A] = new Schema[A] {
    def compile[F[_]](s: Schematic[F]): F[A] = s.struct(make)
  }

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
