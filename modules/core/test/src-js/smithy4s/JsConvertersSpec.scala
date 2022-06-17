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

import munit._

import scala.scalajs.js

class JsConvertersSpec() extends FunSuite {

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
