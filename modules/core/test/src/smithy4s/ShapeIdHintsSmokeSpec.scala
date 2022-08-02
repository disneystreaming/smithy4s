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

import smithy4s.schema._

class ShapeIdHintsSmokeSpec() extends munit.FunSuite {

  type ToShapeIds[A] = List[ShapeId]

  object TestCompiler extends SchemaVisitor.Default[ToShapeIds] {
    def default[A]: List[ShapeId] = Nil

    override def primitive[P](
        shapeId: ShapeId,
        hints: Hints,
        tag: Primitive[P]
    ): ToShapeIds[P] = List(shapeId)

    override def biject[A, B](
        schema: Schema[A],
        bijection: Bijection[A, B]
    ): ToShapeIds[B] = apply[A](schema)

    override def refine[A, B](
        schema: Schema[A],
        refinement: Refinement[A, B]
    ): ToShapeIds[B] = {
      apply[A](schema)
    }

    override def struct[S](
        shapeId: ShapeId,
        hints: Hints,
        fields: Vector[SchemaField[S, _]],
        make: IndexedSeq[Any] => S
    ): ToShapeIds[S] = {
      fields.flatMap(field => apply(field.instance)).toList ++ List(
        shapeId
      )
    }

    override def collection[C[_], A](
        shapeId: ShapeId,
        hints: Hints,
        tag: CollectionTag[C],
        member: Schema[A]
    ): ToShapeIds[C[A]] = apply[A](member) ++ List(shapeId)

    override def union[U](
        shapeId: ShapeId,
        hints: Hints,
        alternatives: Vector[SchemaAlt[U, _]],
        dispatch: Alt.Dispatcher[Schema, U]
    ): ToShapeIds[U] = {
      alternatives.flatMap(field => apply(field.instance)).toList ++ List(
        shapeId
      )
    }
  }

  test("newtypes contain ShapeId in hints") {
    val shapeIds = example.CityId.schema.compile(TestCompiler)
    println(s"YEAH $shapeIds")
    expect(
      shapeIds.contains(
        ShapeId(
          "smithy4s.example",
          "CityId"
        )
      )
    )
  }

  test("structure members contain ShapeId in hints") {
    val shapeIds =
      example.CityCoordinates.schema.compile(TestCompiler).toSet
    expect(
      Set(
        ShapeId("smithy.api", "Float"),
        ShapeId("smithy4s.example", "CityCoordinates")
      ).subsetOf(shapeIds)
    )
  }

  test("union members contain ShapeId in hints") {
    val shapeIds =
      example.ForecastResult.schema.compile(TestCompiler).toSet
    expect(
      Set(
        ShapeId("smithy4s.example", "ForecastResult"),
        ShapeId("smithy4s.example", "ChanceOfRain"),
        ShapeId("smithy4s.example", "UVIndex")
      ).subsetOf(shapeIds)
    )
  }

  test("List items contain ShapeId in hints") {
    val shapeIds =
      example.ListCitiesOutput.schema.compile(TestCompiler).toSet
    expect(
      Set(
        ShapeId("smithy4s.example", "ListCitiesOutput"),
        ShapeId("smithy4s.example", "CitySummary"),
        ShapeId("smithy4s.example", "CityId")
      ).subsetOf(shapeIds)
    )
  }

}
