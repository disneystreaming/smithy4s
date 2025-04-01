/*
 *  Copyright 2021-2025 Disney Streaming
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
package json

import com.github.plokhotnyuk.jsoniter_scala.core.{readFromString => _, _}
import smithy4s.schema.Schema
import smithy4s.schema.Schema._
import munit._

class SchemaVisitorJCodecCustomisationTests extends FunSuite {

  case class FooInt(i: Int)
  object FooInt {
    implicit val schema: Schema[FooInt] = {
      val i = int.required[FooInt]("i", _.i)
      struct(i)(FooInt.apply)
    }
  }

  case class FooShort(s: Short)
  object FooShort {
    implicit val schema: Schema[FooShort] = {
      val s = short.required[FooShort]("s", _.s)
      struct(s)(FooShort.apply)
    }
  }

  case class FooLong(l: Long)
  object FooLong {
    implicit val schema: Schema[FooLong] = {
      val s = long.required[FooLong]("l", _.l)
      struct(s)(FooLong.apply)
    }
  }

  case class FooDouble(d: Double)
  object FooDouble {
    implicit val schema: Schema[FooDouble] = {
      val d = double.required[FooDouble]("d", _.d)
      struct(d)(FooDouble.apply)
    }
  }

  case class FooFloat(f: Float)
  object FooFloat {
    implicit val schema: Schema[FooFloat] = {
      val f = float.required[FooFloat]("f", _.f)
      struct(f)(FooFloat.apply)
    }
  }

  val customJsonCodecs = Json.jsoniter
    .withInfinitySupport(true)
    .withFlexibleCollectionsSupport(true)

  implicit def jsonCodec[A: Schema]: JsonCodec[A] =
    customJsonCodecs.fromSchema(implicitly[Schema[A]])

  private val readerConfig: ReaderConfig = ReaderConfig
    .withThrowReaderExceptionWithStackTrace(true)
    .withAppendHexDumpToParseException(true)
    .withCheckForEndOfInput(false)

  def readFromString[A: JsonCodec](str: String): A = {
    com.github.plokhotnyuk.jsoniter_scala.core
      .readFromString[A](str, readerConfig)
  }

  test("encoding a normal Double") {
    val fd = FooDouble(8.0)
    val result = writeToString[FooDouble](fd)
    val expected = """{"d":8.0}"""

    expect.same(expected, result)
  }

  test("decoding a normal Double") {
    val input = """{"d" : 8.0}"""
    val result = readFromString[FooDouble](input)
    val expected = FooDouble(8.0)

    expect.same(expected, result)
  }

  test("encoding a Double as NaN") {
    val fd = FooDouble(Double.NaN)
    val result = writeToString[FooDouble](fd)
    val expected = """{"d":"NaN"}"""

    expect.same(expected, result)
  }

  test("decoding NaN as a Double") {
    val input = """{"d" : "NaN"}"""
    val result = readFromString[FooDouble](input)

    expect(result.d.isNaN())
  }

  test("encoding Infinity as a Double") {
    val fd = FooDouble(Double.PositiveInfinity)
    val result = writeToString[FooDouble](fd)
    val expected = """{"d":"Infinity"}"""

    expect.same(expected, result)
  }

  test("decoding Infinity as a Double") {
    val input = """{"d" : "Infinity"}"""
    val result = readFromString[FooDouble](input)

    expect(result.d.isPosInfinity)
  }

  test("encoding -Infinity as a Double") {
    val fd = FooDouble(Double.NegativeInfinity)
    val result = writeToString[FooDouble](fd)
    val expected = """{"d":"-Infinity"}"""

    expect.same(expected, result)
  }

  test("decoding -Infinity as a Double") {
    val input = """{"d" : "-Infinity"}"""
    val result = readFromString[FooDouble](input)

    expect(result.d.isNegInfinity)
  }

  test("encoding a normal Float") {
    val ff = FooFloat(12.34f)
    val result = writeToString[FooFloat](ff)
    val expected = """{"f":12.34}"""

    expect.same(expected, result)
  }

  test("decoding a normal Float") {
    val input = """{"f" : 12.34}"""
    val result = readFromString[FooFloat](input)
    val expected = FooFloat(12.34f)

    expect.same(expected, result)
  }

  test("encoding NaN as a Float") {
    val ff = FooFloat(Float.NaN)
    val result = writeToString[FooFloat](ff)
    val expected = """{"f":"NaN"}"""

    expect.same(expected, result)
  }

  test("decoding NaN as a Float") {
    val input = """{"f" : "NaN"}"""
    val result = readFromString[FooFloat](input)

    expect(result.f.isNaN())
  }

  test("encoding Infinity as a Float") {
    val ff = FooFloat(Float.PositiveInfinity)
    val result = writeToString[FooFloat](ff)
    val expected = """{"f":"Infinity"}"""

    expect.same(expected, result)
  }

  test("decoding Infinity as a Float") {
    val input = """{"f" : "Infinity"}"""
    val result = readFromString[FooFloat](input)

    expect(result.f.isPosInfinity)
  }

  test("encoding -Infinity as a Float") {
    val ff = FooFloat(Float.NegativeInfinity)
    val result = writeToString[FooFloat](ff)
    val expected = """{"f":"-Infinity"}"""

    expect.same(expected, result)
  }

  test("decoding -Infinity as a Float") {
    val input = """{"f" : "-Infinity" }"""
    val result = readFromString[FooFloat](input)

    expect(result.f.isNegInfinity)
  }

  test("decoding JSON string as a Float") {
    val input = """{"f" : "1.1" }"""
    val result = readFromString[FooFloat](input)

    expect.eql(result.f, 1.1f)
  }

  test("decoding JSON string as a Double") {
    val input = """{"d" : "1.1" }"""
    val result = readFromString[FooDouble](input)

    expect.eql(result.d, 1.1d)
  }

  test("decoding JSON string as an Int") {
    val input = """{"i" : "1" }"""
    val result = readFromString[FooInt](input)

    expect.eql(result.i, 1)
  }

  test("decoding JSON string as a Long") {
    val input = """{"l" : "1" }"""
    val result = readFromString[FooLong](input)

    expect.eql(result.l, 1L)
  }

  test("decoding JSON string as a Short") {
    val input = """{"s" : "1" }"""
    val result = readFromString[FooShort](input)

    expect.eql(result.s, 1.toShort)
  }
}
