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

import smithy4s.schema._

class ShapeIdHintsSmokeSpec() extends munit.FunSuite {

  type ToShapeIds[A] = List[ShapeId]

  object TestCompiler extends StubSchematic[ToShapeIds] {
    def default[A]: List[ShapeId] = Nil

    override def bijection[A, B](
        f: ToShapeIds[A],
        to: A => B,
        from: B => A
    ): ToShapeIds[B] = f

    override def struct[S](fields: Vector[Field[ToShapeIds, S, _]])(
        const: Vector[Any] => S
    ): ToShapeIds[S] =
      fields.flatMap(_.instance).toList

    override def list[S](fs: ToShapeIds[S]): ToShapeIds[List[S]] = fs

    override def union[S](
        first: Alt[ToShapeIds, S, _],
        rest: Vector[Alt[ToShapeIds, S, _]]
    )(total: S => Alt.WithValue[ToShapeIds, S, _]): ToShapeIds[S] = {
      first.instance ++ rest.flatMap(_.instance)
    }

    override def withHints[A](fa: ToShapeIds[A], hints: Hints): ToShapeIds[A] =
      fa ++ hints.get(ShapeId)
  }

  test("newtypes contain ShapeId in hints") {
    val shapeIds = example.CityId.schema.compile(TestCompiler)
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
