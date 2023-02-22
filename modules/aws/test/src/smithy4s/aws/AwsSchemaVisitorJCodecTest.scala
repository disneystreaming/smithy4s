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

package smithy4s.aws

import com.github.plokhotnyuk.jsoniter_scala.core.{readFromString => _, _}
import smithy4s.aws.json.AwsSchemaVisitorJCodec
import smithy4s.http.json.JCodec
import smithy4s.schema.CompilationCache
import smithy4s.schema.Schema
import smithy4s.schema.Schema._
import smithy4s.schema.SchemaVisitor
import weaver._

object AwsSchemaVisitorJCodecTest extends FunSuite {
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

  val cache = CompilationCache.nop[JCodec]
  val awsSchemaVisitor: SchemaVisitor[JCodec] = new AwsSchemaVisitorJCodec(
    cache = cache
  )

  implicit def awsJsonCodec[A: Schema]: JCodec[A] =
    implicitly[Schema[A]].compile(awsSchemaVisitor)

  private val readerConfig: ReaderConfig = ReaderConfig
    .withThrowReaderExceptionWithStackTrace(true)
    .withAppendHexDumpToParseException(true)
    .withCheckForEndOfInput(false)

  def readFromString[A: JCodec](str: String): A = {
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
}
