package smithy4s

import weaver._
import scala.scalajs.js

object JsConvertersSpec extends FunSuite {

  test("object - strings") {
    val input = js.Dictionary("one" -> "1")
    val result = JsConverters.convertJsToDocument(input)
    val expected =
      Right(Document.DObject(Map("one" -> Document.fromString("1"))))
    expect(result == expected)
  }

  test("array - booleans") {
    val input = js.Array(true, false)
    val result = JsConverters.convertJsToDocument(input)
    val expected =
      Right(Document.DArray(Vector(true, false).map(Document.fromBoolean)))
    expect(result == expected)
  }

  test("integer") {
    val input = 1
    val result = JsConverters.convertJsToDocument(input)
    val expected = Right(Document.fromInt(1))
    expect(result == expected)
  }

  test("null") {
    val input = null
    val result = JsConverters.convertJsToDocument(input)
    val expected =
      Right(Document.DNull)
    expect(result == expected)
  }

  test("undefined") {
    val input = js.undefined
    val result = JsConverters.convertJsToDocument(input)
    val expected =
      Right(Document.DNull)
    expect(result == expected)
  }

  test("double") {
    val input = 1.1
    val result = JsConverters.convertJsToDocument(input)
    val expected =
      Right(Document.fromDouble(1.1))
    expect(result == expected)
  }

  test("float") {
    val input = 1.1f
    val result = JsConverters.convertJsToDocument(input)
    val expected = Right(Document.fromDouble(input.toDouble))
    expect(result == expected)
  }

}
