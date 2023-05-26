package smithy4s.caliban

import weaver._
import CalibanTestUtils._
import smithy4s.example.CityCoordinates
import io.circe.syntax._
import io.circe.Json
import smithy4s.example.MovieTheater
import smithy4s.example.Foo
import smithy4s.Schema
import smithy.api.Length
import smithy4s.Bijection

object CalibanSchemaTests extends SimpleIOSuite {
  test("structure schema - all fields required") {
    testQueryResultWithSchema(
      CityCoordinates(latitude = 42.0f, longitude = 21.37f),
      """query {
        |  latitude
        |  longitude
        |}""".stripMargin
    ).map(
      assert.eql(
        _,
        Json.obj(
          "latitude" := 42.0f,
          "longitude" := 21.37f
        )
      )
    )
  }

  test("structure schema - missing optional field") {
    testQueryResultWithSchema(
      MovieTheater(name = None),
      """query {
        |  name
        |}""".stripMargin
    )
      .map(assert.eql(_, Json.obj("name" := Json.Null)))
  }

  test("union schema") {
    testQueryResultWithSchema(
      Foo.StrCase("myString"): Foo,
      """query {
        |  ... on FoostrCase {
        |    str
        |  }
        |}""".stripMargin
    )
      .map(assert.eql(_, Json.obj("str" := "myString")))
  }

  test("list schema") {

    val schema = Schema.struct[List[String]](
      Schema.list(Schema.string).required[List[String]]("items", identity)
    )(identity(_))

    testQueryResultWithSchema(
      List("a", "b", "c"),
      """query {
        |  items
        |}""".stripMargin
    )(schema)
      .map(assert.eql(_, Json.obj("items" := List("a", "b", "c").asJson)))
  }

  test("refinement schema") {
    val schema = Schema.struct[String](
      Schema.string
        .refined(Length(min = Some(1)))
        .required[String]("item", identity)
    )(identity(_))

    testQueryResultWithSchema(
      "test",
      """query {
        |  item
        |}""".stripMargin
    )(schema)
      .map(assert.eql(_, Json.obj("item" := "test")))
  }

  test("bijection schema") {
    val schema = Schema.struct[String](
      Schema.string
        .biject(Bijection[String, String](identity, _.toUpperCase()))
        .required[String]("item", identity(_))
    )(identity(_))

    testQueryResultWithSchema(
      "test",
      """query {
        |  item
        |}""".stripMargin
    )(schema)
      .map(assert.eql(_, Json.obj("item" := "TEST")))
  }

}
