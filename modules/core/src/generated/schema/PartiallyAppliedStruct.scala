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

/////// THIS FILE WAS GENERATED AT BUILD TIME, AND CHECKED-IN FOR DISCOVERABILITY ///////

package smithy4s
package schema

class PartiallyAppliedStruct[S] protected[schema] (placeholder: ShapeId) {

  def genericArity(fields: SchemaField[S, _]*)(
      const: IndexedSeq[Any] => S
  ): Schema[S] =
    Schema.StructSchema(placeholder, Hints.empty, fields.toVector, const)

  def apply(fields: Vector[SchemaField[S, _]])(
      const: IndexedSeq[Any] => S
  ): Schema[S] =
    Schema.StructSchema(placeholder, Hints.empty, fields, const)

  def apply[A0](a0: SchemaField[S, A0])(const: (A0) => S): Schema[S] =
    Schema.StructSchema[S](
      placeholder,
      Hints.empty,
      Vector(a0),
      arr => const(arr(0).asInstanceOf[A0])
    )
  def apply[A0, A1](a0: SchemaField[S, A0], a1: SchemaField[S, A1])(
      const: (A0, A1) => S
  ): Schema[S] = Schema.StructSchema[S](
    placeholder,
    Hints.empty,
    Vector(a0, a1),
    arr => const(arr(0).asInstanceOf[A0], arr(1).asInstanceOf[A1])
  )
  def apply[A0, A1, A2](
      a0: SchemaField[S, A0],
      a1: SchemaField[S, A1],
      a2: SchemaField[S, A2]
  )(const: (A0, A1, A2) => S): Schema[S] = Schema.StructSchema[S](
    placeholder,
    Hints.empty,
    Vector(a0, a1, a2),
    arr =>
      const(
        arr(0).asInstanceOf[A0],
        arr(1).asInstanceOf[A1],
        arr(2).asInstanceOf[A2]
      )
  )
  def apply[A0, A1, A2, A3](
      a0: SchemaField[S, A0],
      a1: SchemaField[S, A1],
      a2: SchemaField[S, A2],
      a3: SchemaField[S, A3]
  )(const: (A0, A1, A2, A3) => S): Schema[S] = Schema.StructSchema[S](
    placeholder,
    Hints.empty,
    Vector(a0, a1, a2, a3),
    arr =>
      const(
        arr(0).asInstanceOf[A0],
        arr(1).asInstanceOf[A1],
        arr(2).asInstanceOf[A2],
        arr(3).asInstanceOf[A3]
      )
  )
  def apply[A0, A1, A2, A3, A4](
      a0: SchemaField[S, A0],
      a1: SchemaField[S, A1],
      a2: SchemaField[S, A2],
      a3: SchemaField[S, A3],
      a4: SchemaField[S, A4]
  )(const: (A0, A1, A2, A3, A4) => S): Schema[S] = Schema.StructSchema[S](
    placeholder,
    Hints.empty,
    Vector(a0, a1, a2, a3, a4),
    arr =>
      const(
        arr(0).asInstanceOf[A0],
        arr(1).asInstanceOf[A1],
        arr(2).asInstanceOf[A2],
        arr(3).asInstanceOf[A3],
        arr(4).asInstanceOf[A4]
      )
  )
  def apply[A0, A1, A2, A3, A4, A5](
      a0: SchemaField[S, A0],
      a1: SchemaField[S, A1],
      a2: SchemaField[S, A2],
      a3: SchemaField[S, A3],
      a4: SchemaField[S, A4],
      a5: SchemaField[S, A5]
  )(const: (A0, A1, A2, A3, A4, A5) => S): Schema[S] = Schema.StructSchema[S](
    placeholder,
    Hints.empty,
    Vector(a0, a1, a2, a3, a4, a5),
    arr =>
      const(
        arr(0).asInstanceOf[A0],
        arr(1).asInstanceOf[A1],
        arr(2).asInstanceOf[A2],
        arr(3).asInstanceOf[A3],
        arr(4).asInstanceOf[A4],
        arr(5).asInstanceOf[A5]
      )
  )
  def apply[A0, A1, A2, A3, A4, A5, A6](
      a0: SchemaField[S, A0],
      a1: SchemaField[S, A1],
      a2: SchemaField[S, A2],
      a3: SchemaField[S, A3],
      a4: SchemaField[S, A4],
      a5: SchemaField[S, A5],
      a6: SchemaField[S, A6]
  )(const: (A0, A1, A2, A3, A4, A5, A6) => S): Schema[S] =
    Schema.StructSchema[S](
      placeholder,
      Hints.empty,
      Vector(a0, a1, a2, a3, a4, a5, a6),
      arr =>
        const(
          arr(0).asInstanceOf[A0],
          arr(1).asInstanceOf[A1],
          arr(2).asInstanceOf[A2],
          arr(3).asInstanceOf[A3],
          arr(4).asInstanceOf[A4],
          arr(5).asInstanceOf[A5],
          arr(6).asInstanceOf[A6]
        )
    )
  def apply[A0, A1, A2, A3, A4, A5, A6, A7](
      a0: SchemaField[S, A0],
      a1: SchemaField[S, A1],
      a2: SchemaField[S, A2],
      a3: SchemaField[S, A3],
      a4: SchemaField[S, A4],
      a5: SchemaField[S, A5],
      a6: SchemaField[S, A6],
      a7: SchemaField[S, A7]
  )(const: (A0, A1, A2, A3, A4, A5, A6, A7) => S): Schema[S] =
    Schema.StructSchema[S](
      placeholder,
      Hints.empty,
      Vector(a0, a1, a2, a3, a4, a5, a6, a7),
      arr =>
        const(
          arr(0).asInstanceOf[A0],
          arr(1).asInstanceOf[A1],
          arr(2).asInstanceOf[A2],
          arr(3).asInstanceOf[A3],
          arr(4).asInstanceOf[A4],
          arr(5).asInstanceOf[A5],
          arr(6).asInstanceOf[A6],
          arr(7).asInstanceOf[A7]
        )
    )
  def apply[A0, A1, A2, A3, A4, A5, A6, A7, A8](
      a0: SchemaField[S, A0],
      a1: SchemaField[S, A1],
      a2: SchemaField[S, A2],
      a3: SchemaField[S, A3],
      a4: SchemaField[S, A4],
      a5: SchemaField[S, A5],
      a6: SchemaField[S, A6],
      a7: SchemaField[S, A7],
      a8: SchemaField[S, A8]
  )(const: (A0, A1, A2, A3, A4, A5, A6, A7, A8) => S): Schema[S] =
    Schema.StructSchema[S](
      placeholder,
      Hints.empty,
      Vector(a0, a1, a2, a3, a4, a5, a6, a7, a8),
      arr =>
        const(
          arr(0).asInstanceOf[A0],
          arr(1).asInstanceOf[A1],
          arr(2).asInstanceOf[A2],
          arr(3).asInstanceOf[A3],
          arr(4).asInstanceOf[A4],
          arr(5).asInstanceOf[A5],
          arr(6).asInstanceOf[A6],
          arr(7).asInstanceOf[A7],
          arr(8).asInstanceOf[A8]
        )
    )
  def apply[A0, A1, A2, A3, A4, A5, A6, A7, A8, A9](
      a0: SchemaField[S, A0],
      a1: SchemaField[S, A1],
      a2: SchemaField[S, A2],
      a3: SchemaField[S, A3],
      a4: SchemaField[S, A4],
      a5: SchemaField[S, A5],
      a6: SchemaField[S, A6],
      a7: SchemaField[S, A7],
      a8: SchemaField[S, A8],
      a9: SchemaField[S, A9]
  )(const: (A0, A1, A2, A3, A4, A5, A6, A7, A8, A9) => S): Schema[S] =
    Schema.StructSchema[S](
      placeholder,
      Hints.empty,
      Vector(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9),
      arr =>
        const(
          arr(0).asInstanceOf[A0],
          arr(1).asInstanceOf[A1],
          arr(2).asInstanceOf[A2],
          arr(3).asInstanceOf[A3],
          arr(4).asInstanceOf[A4],
          arr(5).asInstanceOf[A5],
          arr(6).asInstanceOf[A6],
          arr(7).asInstanceOf[A7],
          arr(8).asInstanceOf[A8],
          arr(9).asInstanceOf[A9]
        )
    )
  def apply[A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10](
      a0: SchemaField[S, A0],
      a1: SchemaField[S, A1],
      a2: SchemaField[S, A2],
      a3: SchemaField[S, A3],
      a4: SchemaField[S, A4],
      a5: SchemaField[S, A5],
      a6: SchemaField[S, A6],
      a7: SchemaField[S, A7],
      a8: SchemaField[S, A8],
      a9: SchemaField[S, A9],
      a10: SchemaField[S, A10]
  )(const: (A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10) => S): Schema[S] =
    Schema.StructSchema[S](
      placeholder,
      Hints.empty,
      Vector(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10),
      arr =>
        const(
          arr(0).asInstanceOf[A0],
          arr(1).asInstanceOf[A1],
          arr(2).asInstanceOf[A2],
          arr(3).asInstanceOf[A3],
          arr(4).asInstanceOf[A4],
          arr(5).asInstanceOf[A5],
          arr(6).asInstanceOf[A6],
          arr(7).asInstanceOf[A7],
          arr(8).asInstanceOf[A8],
          arr(9).asInstanceOf[A9],
          arr(10).asInstanceOf[A10]
        )
    )
  def apply[A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11](
      a0: SchemaField[S, A0],
      a1: SchemaField[S, A1],
      a2: SchemaField[S, A2],
      a3: SchemaField[S, A3],
      a4: SchemaField[S, A4],
      a5: SchemaField[S, A5],
      a6: SchemaField[S, A6],
      a7: SchemaField[S, A7],
      a8: SchemaField[S, A8],
      a9: SchemaField[S, A9],
      a10: SchemaField[S, A10],
      a11: SchemaField[S, A11]
  )(const: (A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11) => S): Schema[S] =
    Schema.StructSchema[S](
      placeholder,
      Hints.empty,
      Vector(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11),
      arr =>
        const(
          arr(0).asInstanceOf[A0],
          arr(1).asInstanceOf[A1],
          arr(2).asInstanceOf[A2],
          arr(3).asInstanceOf[A3],
          arr(4).asInstanceOf[A4],
          arr(5).asInstanceOf[A5],
          arr(6).asInstanceOf[A6],
          arr(7).asInstanceOf[A7],
          arr(8).asInstanceOf[A8],
          arr(9).asInstanceOf[A9],
          arr(10).asInstanceOf[A10],
          arr(11).asInstanceOf[A11]
        )
    )
  def apply[A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12](
      a0: SchemaField[S, A0],
      a1: SchemaField[S, A1],
      a2: SchemaField[S, A2],
      a3: SchemaField[S, A3],
      a4: SchemaField[S, A4],
      a5: SchemaField[S, A5],
      a6: SchemaField[S, A6],
      a7: SchemaField[S, A7],
      a8: SchemaField[S, A8],
      a9: SchemaField[S, A9],
      a10: SchemaField[S, A10],
      a11: SchemaField[S, A11],
      a12: SchemaField[S, A12]
  )(
      const: (A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12) => S
  ): Schema[S] = Schema.StructSchema[S](
    placeholder,
    Hints.empty,
    Vector(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12),
    arr =>
      const(
        arr(0).asInstanceOf[A0],
        arr(1).asInstanceOf[A1],
        arr(2).asInstanceOf[A2],
        arr(3).asInstanceOf[A3],
        arr(4).asInstanceOf[A4],
        arr(5).asInstanceOf[A5],
        arr(6).asInstanceOf[A6],
        arr(7).asInstanceOf[A7],
        arr(8).asInstanceOf[A8],
        arr(9).asInstanceOf[A9],
        arr(10).asInstanceOf[A10],
        arr(11).asInstanceOf[A11],
        arr(12).asInstanceOf[A12]
      )
  )
  def apply[A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13](
      a0: SchemaField[S, A0],
      a1: SchemaField[S, A1],
      a2: SchemaField[S, A2],
      a3: SchemaField[S, A3],
      a4: SchemaField[S, A4],
      a5: SchemaField[S, A5],
      a6: SchemaField[S, A6],
      a7: SchemaField[S, A7],
      a8: SchemaField[S, A8],
      a9: SchemaField[S, A9],
      a10: SchemaField[S, A10],
      a11: SchemaField[S, A11],
      a12: SchemaField[S, A12],
      a13: SchemaField[S, A13]
  )(
      const: (A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13) => S
  ): Schema[S] = Schema.StructSchema[S](
    placeholder,
    Hints.empty,
    Vector(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13),
    arr =>
      const(
        arr(0).asInstanceOf[A0],
        arr(1).asInstanceOf[A1],
        arr(2).asInstanceOf[A2],
        arr(3).asInstanceOf[A3],
        arr(4).asInstanceOf[A4],
        arr(5).asInstanceOf[A5],
        arr(6).asInstanceOf[A6],
        arr(7).asInstanceOf[A7],
        arr(8).asInstanceOf[A8],
        arr(9).asInstanceOf[A9],
        arr(10).asInstanceOf[A10],
        arr(11).asInstanceOf[A11],
        arr(12).asInstanceOf[A12],
        arr(13).asInstanceOf[A13]
      )
  )
  def apply[A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14](
      a0: SchemaField[S, A0],
      a1: SchemaField[S, A1],
      a2: SchemaField[S, A2],
      a3: SchemaField[S, A3],
      a4: SchemaField[S, A4],
      a5: SchemaField[S, A5],
      a6: SchemaField[S, A6],
      a7: SchemaField[S, A7],
      a8: SchemaField[S, A8],
      a9: SchemaField[S, A9],
      a10: SchemaField[S, A10],
      a11: SchemaField[S, A11],
      a12: SchemaField[S, A12],
      a13: SchemaField[S, A13],
      a14: SchemaField[S, A14]
  )(
      const: (
          A0,
          A1,
          A2,
          A3,
          A4,
          A5,
          A6,
          A7,
          A8,
          A9,
          A10,
          A11,
          A12,
          A13,
          A14
      ) => S
  ): Schema[S] = Schema.StructSchema[S](
    placeholder,
    Hints.empty,
    Vector(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14),
    arr =>
      const(
        arr(0).asInstanceOf[A0],
        arr(1).asInstanceOf[A1],
        arr(2).asInstanceOf[A2],
        arr(3).asInstanceOf[A3],
        arr(4).asInstanceOf[A4],
        arr(5).asInstanceOf[A5],
        arr(6).asInstanceOf[A6],
        arr(7).asInstanceOf[A7],
        arr(8).asInstanceOf[A8],
        arr(9).asInstanceOf[A9],
        arr(10).asInstanceOf[A10],
        arr(11).asInstanceOf[A11],
        arr(12).asInstanceOf[A12],
        arr(13).asInstanceOf[A13],
        arr(14).asInstanceOf[A14]
      )
  )
  def apply[
      A0,
      A1,
      A2,
      A3,
      A4,
      A5,
      A6,
      A7,
      A8,
      A9,
      A10,
      A11,
      A12,
      A13,
      A14,
      A15
  ](
      a0: SchemaField[S, A0],
      a1: SchemaField[S, A1],
      a2: SchemaField[S, A2],
      a3: SchemaField[S, A3],
      a4: SchemaField[S, A4],
      a5: SchemaField[S, A5],
      a6: SchemaField[S, A6],
      a7: SchemaField[S, A7],
      a8: SchemaField[S, A8],
      a9: SchemaField[S, A9],
      a10: SchemaField[S, A10],
      a11: SchemaField[S, A11],
      a12: SchemaField[S, A12],
      a13: SchemaField[S, A13],
      a14: SchemaField[S, A14],
      a15: SchemaField[S, A15]
  )(
      const: (
          A0,
          A1,
          A2,
          A3,
          A4,
          A5,
          A6,
          A7,
          A8,
          A9,
          A10,
          A11,
          A12,
          A13,
          A14,
          A15
      ) => S
  ): Schema[S] = Schema.StructSchema[S](
    placeholder,
    Hints.empty,
    Vector(
      a0,
      a1,
      a2,
      a3,
      a4,
      a5,
      a6,
      a7,
      a8,
      a9,
      a10,
      a11,
      a12,
      a13,
      a14,
      a15
    ),
    arr =>
      const(
        arr(0).asInstanceOf[A0],
        arr(1).asInstanceOf[A1],
        arr(2).asInstanceOf[A2],
        arr(3).asInstanceOf[A3],
        arr(4).asInstanceOf[A4],
        arr(5).asInstanceOf[A5],
        arr(6).asInstanceOf[A6],
        arr(7).asInstanceOf[A7],
        arr(8).asInstanceOf[A8],
        arr(9).asInstanceOf[A9],
        arr(10).asInstanceOf[A10],
        arr(11).asInstanceOf[A11],
        arr(12).asInstanceOf[A12],
        arr(13).asInstanceOf[A13],
        arr(14).asInstanceOf[A14],
        arr(15).asInstanceOf[A15]
      )
  )
  def apply[
      A0,
      A1,
      A2,
      A3,
      A4,
      A5,
      A6,
      A7,
      A8,
      A9,
      A10,
      A11,
      A12,
      A13,
      A14,
      A15,
      A16
  ](
      a0: SchemaField[S, A0],
      a1: SchemaField[S, A1],
      a2: SchemaField[S, A2],
      a3: SchemaField[S, A3],
      a4: SchemaField[S, A4],
      a5: SchemaField[S, A5],
      a6: SchemaField[S, A6],
      a7: SchemaField[S, A7],
      a8: SchemaField[S, A8],
      a9: SchemaField[S, A9],
      a10: SchemaField[S, A10],
      a11: SchemaField[S, A11],
      a12: SchemaField[S, A12],
      a13: SchemaField[S, A13],
      a14: SchemaField[S, A14],
      a15: SchemaField[S, A15],
      a16: SchemaField[S, A16]
  )(
      const: (
          A0,
          A1,
          A2,
          A3,
          A4,
          A5,
          A6,
          A7,
          A8,
          A9,
          A10,
          A11,
          A12,
          A13,
          A14,
          A15,
          A16
      ) => S
  ): Schema[S] = Schema.StructSchema[S](
    placeholder,
    Hints.empty,
    Vector(
      a0,
      a1,
      a2,
      a3,
      a4,
      a5,
      a6,
      a7,
      a8,
      a9,
      a10,
      a11,
      a12,
      a13,
      a14,
      a15,
      a16
    ),
    arr =>
      const(
        arr(0).asInstanceOf[A0],
        arr(1).asInstanceOf[A1],
        arr(2).asInstanceOf[A2],
        arr(3).asInstanceOf[A3],
        arr(4).asInstanceOf[A4],
        arr(5).asInstanceOf[A5],
        arr(6).asInstanceOf[A6],
        arr(7).asInstanceOf[A7],
        arr(8).asInstanceOf[A8],
        arr(9).asInstanceOf[A9],
        arr(10).asInstanceOf[A10],
        arr(11).asInstanceOf[A11],
        arr(12).asInstanceOf[A12],
        arr(13).asInstanceOf[A13],
        arr(14).asInstanceOf[A14],
        arr(15).asInstanceOf[A15],
        arr(16).asInstanceOf[A16]
      )
  )
  def apply[
      A0,
      A1,
      A2,
      A3,
      A4,
      A5,
      A6,
      A7,
      A8,
      A9,
      A10,
      A11,
      A12,
      A13,
      A14,
      A15,
      A16,
      A17
  ](
      a0: SchemaField[S, A0],
      a1: SchemaField[S, A1],
      a2: SchemaField[S, A2],
      a3: SchemaField[S, A3],
      a4: SchemaField[S, A4],
      a5: SchemaField[S, A5],
      a6: SchemaField[S, A6],
      a7: SchemaField[S, A7],
      a8: SchemaField[S, A8],
      a9: SchemaField[S, A9],
      a10: SchemaField[S, A10],
      a11: SchemaField[S, A11],
      a12: SchemaField[S, A12],
      a13: SchemaField[S, A13],
      a14: SchemaField[S, A14],
      a15: SchemaField[S, A15],
      a16: SchemaField[S, A16],
      a17: SchemaField[S, A17]
  )(
      const: (
          A0,
          A1,
          A2,
          A3,
          A4,
          A5,
          A6,
          A7,
          A8,
          A9,
          A10,
          A11,
          A12,
          A13,
          A14,
          A15,
          A16,
          A17
      ) => S
  ): Schema[S] = Schema.StructSchema[S](
    placeholder,
    Hints.empty,
    Vector(
      a0,
      a1,
      a2,
      a3,
      a4,
      a5,
      a6,
      a7,
      a8,
      a9,
      a10,
      a11,
      a12,
      a13,
      a14,
      a15,
      a16,
      a17
    ),
    arr =>
      const(
        arr(0).asInstanceOf[A0],
        arr(1).asInstanceOf[A1],
        arr(2).asInstanceOf[A2],
        arr(3).asInstanceOf[A3],
        arr(4).asInstanceOf[A4],
        arr(5).asInstanceOf[A5],
        arr(6).asInstanceOf[A6],
        arr(7).asInstanceOf[A7],
        arr(8).asInstanceOf[A8],
        arr(9).asInstanceOf[A9],
        arr(10).asInstanceOf[A10],
        arr(11).asInstanceOf[A11],
        arr(12).asInstanceOf[A12],
        arr(13).asInstanceOf[A13],
        arr(14).asInstanceOf[A14],
        arr(15).asInstanceOf[A15],
        arr(16).asInstanceOf[A16],
        arr(17).asInstanceOf[A17]
      )
  )
  def apply[
      A0,
      A1,
      A2,
      A3,
      A4,
      A5,
      A6,
      A7,
      A8,
      A9,
      A10,
      A11,
      A12,
      A13,
      A14,
      A15,
      A16,
      A17,
      A18
  ](
      a0: SchemaField[S, A0],
      a1: SchemaField[S, A1],
      a2: SchemaField[S, A2],
      a3: SchemaField[S, A3],
      a4: SchemaField[S, A4],
      a5: SchemaField[S, A5],
      a6: SchemaField[S, A6],
      a7: SchemaField[S, A7],
      a8: SchemaField[S, A8],
      a9: SchemaField[S, A9],
      a10: SchemaField[S, A10],
      a11: SchemaField[S, A11],
      a12: SchemaField[S, A12],
      a13: SchemaField[S, A13],
      a14: SchemaField[S, A14],
      a15: SchemaField[S, A15],
      a16: SchemaField[S, A16],
      a17: SchemaField[S, A17],
      a18: SchemaField[S, A18]
  )(
      const: (
          A0,
          A1,
          A2,
          A3,
          A4,
          A5,
          A6,
          A7,
          A8,
          A9,
          A10,
          A11,
          A12,
          A13,
          A14,
          A15,
          A16,
          A17,
          A18
      ) => S
  ): Schema[S] = Schema.StructSchema[S](
    placeholder,
    Hints.empty,
    Vector(
      a0,
      a1,
      a2,
      a3,
      a4,
      a5,
      a6,
      a7,
      a8,
      a9,
      a10,
      a11,
      a12,
      a13,
      a14,
      a15,
      a16,
      a17,
      a18
    ),
    arr =>
      const(
        arr(0).asInstanceOf[A0],
        arr(1).asInstanceOf[A1],
        arr(2).asInstanceOf[A2],
        arr(3).asInstanceOf[A3],
        arr(4).asInstanceOf[A4],
        arr(5).asInstanceOf[A5],
        arr(6).asInstanceOf[A6],
        arr(7).asInstanceOf[A7],
        arr(8).asInstanceOf[A8],
        arr(9).asInstanceOf[A9],
        arr(10).asInstanceOf[A10],
        arr(11).asInstanceOf[A11],
        arr(12).asInstanceOf[A12],
        arr(13).asInstanceOf[A13],
        arr(14).asInstanceOf[A14],
        arr(15).asInstanceOf[A15],
        arr(16).asInstanceOf[A16],
        arr(17).asInstanceOf[A17],
        arr(18).asInstanceOf[A18]
      )
  )
  def apply[
      A0,
      A1,
      A2,
      A3,
      A4,
      A5,
      A6,
      A7,
      A8,
      A9,
      A10,
      A11,
      A12,
      A13,
      A14,
      A15,
      A16,
      A17,
      A18,
      A19
  ](
      a0: SchemaField[S, A0],
      a1: SchemaField[S, A1],
      a2: SchemaField[S, A2],
      a3: SchemaField[S, A3],
      a4: SchemaField[S, A4],
      a5: SchemaField[S, A5],
      a6: SchemaField[S, A6],
      a7: SchemaField[S, A7],
      a8: SchemaField[S, A8],
      a9: SchemaField[S, A9],
      a10: SchemaField[S, A10],
      a11: SchemaField[S, A11],
      a12: SchemaField[S, A12],
      a13: SchemaField[S, A13],
      a14: SchemaField[S, A14],
      a15: SchemaField[S, A15],
      a16: SchemaField[S, A16],
      a17: SchemaField[S, A17],
      a18: SchemaField[S, A18],
      a19: SchemaField[S, A19]
  )(
      const: (
          A0,
          A1,
          A2,
          A3,
          A4,
          A5,
          A6,
          A7,
          A8,
          A9,
          A10,
          A11,
          A12,
          A13,
          A14,
          A15,
          A16,
          A17,
          A18,
          A19
      ) => S
  ): Schema[S] = Schema.StructSchema[S](
    placeholder,
    Hints.empty,
    Vector(
      a0,
      a1,
      a2,
      a3,
      a4,
      a5,
      a6,
      a7,
      a8,
      a9,
      a10,
      a11,
      a12,
      a13,
      a14,
      a15,
      a16,
      a17,
      a18,
      a19
    ),
    arr =>
      const(
        arr(0).asInstanceOf[A0],
        arr(1).asInstanceOf[A1],
        arr(2).asInstanceOf[A2],
        arr(3).asInstanceOf[A3],
        arr(4).asInstanceOf[A4],
        arr(5).asInstanceOf[A5],
        arr(6).asInstanceOf[A6],
        arr(7).asInstanceOf[A7],
        arr(8).asInstanceOf[A8],
        arr(9).asInstanceOf[A9],
        arr(10).asInstanceOf[A10],
        arr(11).asInstanceOf[A11],
        arr(12).asInstanceOf[A12],
        arr(13).asInstanceOf[A13],
        arr(14).asInstanceOf[A14],
        arr(15).asInstanceOf[A15],
        arr(16).asInstanceOf[A16],
        arr(17).asInstanceOf[A17],
        arr(18).asInstanceOf[A18],
        arr(19).asInstanceOf[A19]
      )
  )
  def apply[
      A0,
      A1,
      A2,
      A3,
      A4,
      A5,
      A6,
      A7,
      A8,
      A9,
      A10,
      A11,
      A12,
      A13,
      A14,
      A15,
      A16,
      A17,
      A18,
      A19,
      A20
  ](
      a0: SchemaField[S, A0],
      a1: SchemaField[S, A1],
      a2: SchemaField[S, A2],
      a3: SchemaField[S, A3],
      a4: SchemaField[S, A4],
      a5: SchemaField[S, A5],
      a6: SchemaField[S, A6],
      a7: SchemaField[S, A7],
      a8: SchemaField[S, A8],
      a9: SchemaField[S, A9],
      a10: SchemaField[S, A10],
      a11: SchemaField[S, A11],
      a12: SchemaField[S, A12],
      a13: SchemaField[S, A13],
      a14: SchemaField[S, A14],
      a15: SchemaField[S, A15],
      a16: SchemaField[S, A16],
      a17: SchemaField[S, A17],
      a18: SchemaField[S, A18],
      a19: SchemaField[S, A19],
      a20: SchemaField[S, A20]
  )(
      const: (
          A0,
          A1,
          A2,
          A3,
          A4,
          A5,
          A6,
          A7,
          A8,
          A9,
          A10,
          A11,
          A12,
          A13,
          A14,
          A15,
          A16,
          A17,
          A18,
          A19,
          A20
      ) => S
  ): Schema[S] = Schema.StructSchema[S](
    placeholder,
    Hints.empty,
    Vector(
      a0,
      a1,
      a2,
      a3,
      a4,
      a5,
      a6,
      a7,
      a8,
      a9,
      a10,
      a11,
      a12,
      a13,
      a14,
      a15,
      a16,
      a17,
      a18,
      a19,
      a20
    ),
    arr =>
      const(
        arr(0).asInstanceOf[A0],
        arr(1).asInstanceOf[A1],
        arr(2).asInstanceOf[A2],
        arr(3).asInstanceOf[A3],
        arr(4).asInstanceOf[A4],
        arr(5).asInstanceOf[A5],
        arr(6).asInstanceOf[A6],
        arr(7).asInstanceOf[A7],
        arr(8).asInstanceOf[A8],
        arr(9).asInstanceOf[A9],
        arr(10).asInstanceOf[A10],
        arr(11).asInstanceOf[A11],
        arr(12).asInstanceOf[A12],
        arr(13).asInstanceOf[A13],
        arr(14).asInstanceOf[A14],
        arr(15).asInstanceOf[A15],
        arr(16).asInstanceOf[A16],
        arr(17).asInstanceOf[A17],
        arr(18).asInstanceOf[A18],
        arr(19).asInstanceOf[A19],
        arr(20).asInstanceOf[A20]
      )
  )
  def apply[
      A0,
      A1,
      A2,
      A3,
      A4,
      A5,
      A6,
      A7,
      A8,
      A9,
      A10,
      A11,
      A12,
      A13,
      A14,
      A15,
      A16,
      A17,
      A18,
      A19,
      A20,
      A21
  ](
      a0: SchemaField[S, A0],
      a1: SchemaField[S, A1],
      a2: SchemaField[S, A2],
      a3: SchemaField[S, A3],
      a4: SchemaField[S, A4],
      a5: SchemaField[S, A5],
      a6: SchemaField[S, A6],
      a7: SchemaField[S, A7],
      a8: SchemaField[S, A8],
      a9: SchemaField[S, A9],
      a10: SchemaField[S, A10],
      a11: SchemaField[S, A11],
      a12: SchemaField[S, A12],
      a13: SchemaField[S, A13],
      a14: SchemaField[S, A14],
      a15: SchemaField[S, A15],
      a16: SchemaField[S, A16],
      a17: SchemaField[S, A17],
      a18: SchemaField[S, A18],
      a19: SchemaField[S, A19],
      a20: SchemaField[S, A20],
      a21: SchemaField[S, A21]
  )(
      const: (
          A0,
          A1,
          A2,
          A3,
          A4,
          A5,
          A6,
          A7,
          A8,
          A9,
          A10,
          A11,
          A12,
          A13,
          A14,
          A15,
          A16,
          A17,
          A18,
          A19,
          A20,
          A21
      ) => S
  ): Schema[S] = Schema.StructSchema[S](
    placeholder,
    Hints.empty,
    Vector(
      a0,
      a1,
      a2,
      a3,
      a4,
      a5,
      a6,
      a7,
      a8,
      a9,
      a10,
      a11,
      a12,
      a13,
      a14,
      a15,
      a16,
      a17,
      a18,
      a19,
      a20,
      a21
    ),
    arr =>
      const(
        arr(0).asInstanceOf[A0],
        arr(1).asInstanceOf[A1],
        arr(2).asInstanceOf[A2],
        arr(3).asInstanceOf[A3],
        arr(4).asInstanceOf[A4],
        arr(5).asInstanceOf[A5],
        arr(6).asInstanceOf[A6],
        arr(7).asInstanceOf[A7],
        arr(8).asInstanceOf[A8],
        arr(9).asInstanceOf[A9],
        arr(10).asInstanceOf[A10],
        arr(11).asInstanceOf[A11],
        arr(12).asInstanceOf[A12],
        arr(13).asInstanceOf[A13],
        arr(14).asInstanceOf[A14],
        arr(15).asInstanceOf[A15],
        arr(16).asInstanceOf[A16],
        arr(17).asInstanceOf[A17],
        arr(18).asInstanceOf[A18],
        arr(19).asInstanceOf[A19],
        arr(20).asInstanceOf[A20],
        arr(21).asInstanceOf[A21]
      )
  )

}
