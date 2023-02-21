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

import com.github.plokhotnyuk.jsoniter_scala.core.ReaderConfig
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

  test("encoding NaN as a Double") {
    ???
  }

  test("decoding NaN as a Double") {
    ???
  }
  test("encoding Infinity as a Double") {
    ???
  }

  test("decoding Infinity as a Double") {
    ???
  }

  test("encoding -Infinity as a Double") {
    ???
  }

  test("decoding -Infinity as a Double") {
    ???
  }
}
