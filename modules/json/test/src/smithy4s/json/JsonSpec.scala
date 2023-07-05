package smithy4s.json

import munit._
import smithy4s.schema.Schema
import smithy4s.Blob
import smithy4s.Document
import Schema._

class JsonSpec() extends FunSuite {

  case class Foo(a: Int, b: Option[Int])
  object Foo {
    implicit val schema: Schema[Foo] = {
      val a = int.required[Foo]("a", _.a)
      val b = int.optional[Foo]("b", _.b)
      struct(a, b)(Foo.apply)
    }
  }

  test("Json read/write") {
    val foo = Foo(1, Some(2))
    val result = Json.writePrettyString(foo)
    val roundTripped = Json.read[Foo](Blob(result))
    val expectedJson = """|{
                          |  "a": 1,
                          |  "b": 2
                          |}""".stripMargin

    assertEquals(result, expectedJson)
    assertEquals(roundTripped, Right(foo))
  }

  test("Json document read/write") {
    val foo =
      Document.obj("a" -> Document.fromInt(1), "b" -> Document.fromInt(2))
    val result = Json.writeDocumentAsPrettyString(foo)
    val roundTripped = Json.readDocument(Blob(result))
    val expectedJson = """|{
                          |  "a": 1,
                          |  "b": 2
                          |}""".stripMargin

    assertEquals(result, expectedJson)
    assertEquals(roundTripped, Right(foo))
  }

}
