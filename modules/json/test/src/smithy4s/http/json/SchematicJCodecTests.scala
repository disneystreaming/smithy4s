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
package http.json

import com.github.plokhotnyuk.jsoniter_scala.core.{readFromString => _, _}
import weaver._
import smithy.api.JsonName
import smithy4s.http.PayloadError
import smithy4s.syntax._
import smithy4s.api.Discriminated

import scala.collection.immutable.ListMap
import smithy4s.example.PayloadData
import smithy4s.example.TestBiggerUnion
import smithy4s.example.One

object SchematicJCodecTests extends SimpleIOSuite {

  case class Foo(a: Int, b: Option[Int])
  object Foo {
    implicit val schema: Schema[Foo] = {
      val a = int.required[Foo]("a", _.a)
      val b = int.optional[Foo]("b", _.b).addHints(JsonName("_b"))
      struct(a, b)(Foo.apply)
    }
  }

  private val readerConfig: ReaderConfig = ReaderConfig
    .withThrowReaderExceptionWithStackTrace(true)
    .withAppendHexDumpToParseException(true)
    .withCheckForEndOfInput(false)

  def readFromString[A: JsonCodec](str: String): A = {
    com.github.plokhotnyuk.jsoniter_scala.core
      .readFromString[A](str, readerConfig)
  }

  import JCodec.deriveJCodecFromSchema

  case class IntList(head: Int, tail: Option[IntList] = None)
  object IntList {
    val hints: smithy4s.Hints = smithy4s.Hints()

    implicit val schema: smithy4s.Schema[IntList] = recursive(
      struct(
        int.required[IntList]("head", _.head).addHints(smithy.api.Required()),
        IntList.schema.optional[IntList]("tail", _.tail)
      ) {
        IntList.apply
      }
    )
  }

  case class Baz(str: String)
  case class Bin(str: String, int: Int)

  implicit val eitherBazBinSchema: Schema[Either[Baz, Bin]] = {
    val left = struct(string.required[Baz]("str", _.str))(Baz.apply)
      .oneOf[Either[Baz, Bin]]("baz", (f: Baz) => Left(f))

    val right = struct(
      string.required[Bin]("str", _.str).addHints(JsonName("binStr")),
      int.required[Bin]("int", _.int)
    )(Bin.apply)
      .oneOf[Either[Baz, Bin]]("bin", (b: Bin) => Right(b))
      .addHints(JsonName("binBin"))

    union(left, right) {
      case Left(f)  => left(f)
      case Right(b) => right(b)
    }.addHints(
      Discriminated("type")
    )
  }

  pureTest(
    "Compiling a codec for a recursive type should not blow up the stack"
  ) {
    val foo = IntList(1, Some(IntList(2)))
    val json = """{"head":1,"tail":{"head":2}}"""
    val result = writeToString[IntList](foo)
    val roundTripped = readFromString[IntList](json)
    expect(result == json) &&
    expect(roundTripped == foo)
  }

  pureTest("Optional encode from present value") {
    val foo = Foo(1, Some(2))
    val json = """{"a":1,"_b":2}"""
    val result = writeToString[Foo](foo)
    expect(result == json)
  }

  pureTest("Optional decode from present value") {
    val json = """{"a" : 1, "_b": 2}"""
    val result = readFromString[Foo](json)
    expect(result == Foo(1, Some(2)))
  }

  pureTest("Optional decode from absent value") {
    val json = """{"a" : 1}"""
    val result = readFromString[Foo](json)
    expect(result == Foo(1, None))
  }

  pureTest("Optional decode from null value") {
    val json = """{"a" : 1, "_b": null}"""
    val result = readFromString[Foo](json)
    expect(result == Foo(1, None))
  }

  pureTest("Optional: path gets surfaced in errors") {
    val json = """{"a" : 1, "_b": "foo"}"""
    try {
      val _ = readFromString[Foo](json)
      failure("Unexpected success")
    } catch {
      case PayloadError(path, expected, _) =>
        expect(path == PayloadPath("b")) &&
          expect(expected == "int")
    }
  }

  implicit val eitherIntStringSchema: Schema[Either[Int, String]] = {
    val left = int.oneOf[Either[Int, String]]("int", (int: Int) => Left(int))
    val right =
      string
        .oneOf[Either[Int, String]]("string", (str: String) => Right(str))
        .addHints(JsonName("_string"))
    union(left, right) {
      case Left(i)    => left(i)
      case Right(str) => right(str)
    }
  }

  pureTest("Union gets encoded correctly") {
    val jsonInt = """{"int":1}"""
    val jsonStr = """{"_string":"foo"}"""
    val int = writeToString[Either[Int, String]](Left(1))
    val str = writeToString[Either[Int, String]](Right("foo"))
    expect(int == jsonInt) &&
    expect(str == jsonStr)
  }

  pureTest("Discriminated union gets encoded correctly") {
    val jsonBaz = """{"type":"baz","str":"test"}"""
    val jsonBin = """{"type":"binBin","binStr":"foo","int":2022}"""
    val baz = writeToString[Either[Baz, Bin]](Left(Baz("test")))
    val bin = writeToString[Either[Baz, Bin]](Right(Bin("foo", 2022)))
    expect(baz == jsonBaz) &&
    expect(bin == jsonBin)
  }

  pureTest("Discriminated union decoding tolerates whitespace") {
    val json = """ { "tpe" : "one" , "value" : "hello" }"""
    val result = readFromString[TestBiggerUnion](json)

    expect(
      result == TestBiggerUnion.OneCase(One(Some("hello")))
    )
  }

  pureTest("Discriminated union discriminator can follow other keys") {
    val json = """ { "value" : "hello", "tpe" : "one" }"""
    val result = readFromString[TestBiggerUnion](json)

    expect(
      result == TestBiggerUnion.OneCase(One(Some("hello")))
    )
  }

  pureTest("Nested discriminated union decoding tolerates whitespace") {
    val json = """{ "testBiggerUnion": { "tpe": "one", "value": "hello" } }"""
    val result = readFromString[PayloadData](json)

    expect(
      result == PayloadData(
        Some(TestBiggerUnion.OneCase(One(Some("hello"))))
      )
    )
  }

  pureTest("Discriminated union gets routed to the correct codec") {
    val jsonBaz = """{"type":"baz","str":"test"}"""
    val jsonBin = """{"type":"binBin","binStr":"foo","int":2022}"""
    val baz = readFromString[Either[Baz, Bin]](jsonBaz)
    val bin = readFromString[Either[Baz, Bin]](jsonBin)
    expect(baz == Left(Baz("test"))) &&
    expect(bin == Right(Bin("foo", 2022)))
  }

  pureTest("Union gets routed to the correct codec") {
    val jsonInt = """{"int" :  1}"""
    val jsonStr = """{"_string" : "foo"}"""
    val int = readFromString[Either[Int, String]](jsonInt)
    val str = readFromString[Either[Int, String]](jsonStr)
    expect(int == Left(1)) &&
    expect(str == Right("foo"))
  }

  pureTest("Union: path gets surfaced in errors") {
    val json = """{"int" : null}"""
    try {
      val _ = readFromString[Either[Int, String]](json)
      failure("Unexpected success")
    } catch {
      case PayloadError(path, expected, msg) =>
        expect(path == PayloadPath("int")) &&
          expect(expected == "int") &&
          expect(msg.contains("illegal number"))

    }
  }

  pureTest("Union: wrong shape") {
    val json = """null"""
    try {
      val _ = readFromString[Either[Int, String]](json)
      failure("Unexpected success")
    } catch {
      case PayloadError(path, expected, msg) =>
        expect(path == PayloadPath.root) &&
          expect(expected == "tagged-union") &&
          expect(msg.contains("Expected JSON object"))

    }
  }

  implicit val byteArraySchema: Schema[ByteArray] = bytes

  pureTest("byte arrays are encoded as base64") {
    val bytes = ByteArray("foobar".getBytes())
    val bytesJson = writeToString(bytes)
    val decoded = readFromString[ByteArray](bytesJson)
    expect(bytesJson == "\"Zm9vYmFy\"") && expect(decoded == bytes)
  }

  implicit val documentSchema: Schema[Document] = document
  pureTest("documents get encoded as json") {
    import Document._
    val doc: Document = DObject(
      ListMap(
        "int" -> DNumber(BigDecimal(1)),
        "str" -> DString("hello"),
        "null" -> DNull,
        "bool" -> DBoolean(true),
        "array" -> DArray(IndexedSeq(DString("foo"), DString("bar")))
      )
    )
    val documentJson = writeToString(doc)
    val expected =
      """{"int":1,"str":"hello","null":null,"bool":true,"array":["foo","bar"]}"""

    val decoded = readFromString[Document](documentJson)

    expect(documentJson == expected) &&
    expect(decoded == doc)
  }

  case class Bar(
      str: Option[String],
      lst: Option[List[Int]],
      int: Option[Int]
  )
  object Bar {
    val maxLength = 10
    val lengthHint = smithy.api.Length(max = Some(maxLength.toLong))
    val rangeHint = smithy.api.Range(max = Some(maxLength.toLong))
    implicit val schema: Schema[Bar] = {
      val str = string
        .optional[Bar]("str", _.str)
        .addHints(lengthHint)
      val lst = list[Int](int).optional[Bar]("lst", _.lst).addHints(lengthHint)
      val intS = int.optional[Bar]("int", _.int).addHints(rangeHint)
      struct(str, lst, intS)(Bar.apply)
    }
  }

  pureTest("throw PayloadError on String violating length constraint") {
    val str = "a" * (Bar.maxLength + 1)
    val json = s"""{"str":"$str"}"""
    val result = util.Try(readFromString[Bar](json))
    expect(
      result.failed.get.getMessage == "length required to be <= 10, but was 11"
    )
  }

  pureTest("throw PayloadError on Vector violating length constraint") {
    val vct = Vector.fill(Bar.maxLength + 1)(0)
    val json = s"""{"vct": ${vct.mkString("[", ",", "]")}}"""
    val result = util.Try(readFromString[Bar](json))
    expect(
      result.failed.get.getMessage == "length required to be <= 10, but was 11"
    )
  }

  pureTest("throw PayloadError on List violating length constraint") {
    val lst = List.fill(Bar.maxLength + 1)(0)
    val json = s"""{"lst": ${lst.mkString("[", ",", "]")}}"""
    val result = util.Try(readFromString[Bar](json))
    expect(
      result.failed.get.getMessage == "length required to be <= 10, but was 11"
    )
  }

  pureTest("throw PayloadError on Int violating range constraint") {
    val int = Bar.maxLength + 1
    val json = s"""{"int":$int}"""
    val result = util.Try(readFromString[Bar](json))
    expect(
      result.failed.get.getMessage == (if (Platform.isJVM)
                                         "Input must be <= 10, but was 11.0"
                                       else "Input must be <= 10, but was 11")
    )
  }

  case class Bar2(str: String)

  case class Foo2(bar: Bar2)
  object Foo2 {
    val maxLength = 10
    val lengthHint = smithy.api.Length(max = Some(maxLength.toLong))
    implicit val schema: Schema[Foo2] = {
      val str = string
        .required[Bar2]("str", _.str)
        .addHints(lengthHint)
      val bar = struct(str)(Bar2.apply).required[Foo2]("bar", _.bar)
      struct(bar)(Foo2.apply)
    }
  }

  pureTest(
    "throw PayloadError on Struct[Struct[String]] violating length constraint"
  ) {
    val str = "a" * (Foo2.maxLength + 1)
    val json = s"""{"bar":{"str":"$str"}}"""
    val result = util.Try(readFromString[Foo2](json))
    expect(
      result.failed.get.getMessage == "length required to be <= 10, but was 11"
    )
  }

  case class Foo3(bar: List[Bar2])
  object Foo3 {
    val maxLength = 10
    val lengthHint = smithy.api.Length(max = Some(maxLength.toLong))
    implicit val schema: Schema[Foo3] = {
      val str = string
        .required[Bar2]("str", _.str)
        .addHints(lengthHint)
      val bar = list(struct(str)(Bar2.apply)).required[Foo3]("bar", _.bar)
      struct(bar)(Foo3.apply)
    }
  }

  pureTest(
    "throw PayloadError on Struct[List[Struct[String]]] violating length constraint"
  ) {
    try {
      val str = "a" * (Foo3.maxLength + 1)
      val json = s"""{"bar":[{"str":"$str"}]}"""
      val _ = readFromString[Foo3](json)
      failure("Unexpected success")
    } catch {
      case PayloadError(path, _, message) =>
        expect(
          message == "length required to be <= 10, but was 11"
        ) && expect(
          path == PayloadPath.fromString("bar.0.str")
        )
    }
  }

  private implicit val schemaMapStringInt: Schema[Map[String, Int]] = {
    map(string, int)
  }

  pureTest("throw PayloadError on Map inserts over maxArity") {
    try {
      val items =
        List.fill(1025)("1").map(i => s""""$i":$i""").mkString("{", ",", "}")
      val _ = readFromString[Map[String, Int]](items)
      failure("Unexpected success")
    } catch {
      case PayloadError(_, _, message) =>
        expect(message == "input map exceeded max arity of `1024`")
    }
  }

  private implicit val schemaVectorInt: Schema[List[Int]] = {
    list(int)
  }

  pureTest("throw PayloadError on Vector inserts over maxArity") {
    try {
      val items = List.fill(1025)("1").mkString("[", ",", "]")
      val _ = readFromString[List[Int]](items)
      failure("Unexpected success")
    } catch {
      case PayloadError(_, _, message) =>
        expect(message == "input list exceeded max arity of `1024`")
    }
  }

  pureTest("throw PayloadError on Document list inserts over maxArity") {
    try {
      val items = List.fill(1025)("1").mkString("[", ",", "]")
      val _ = readFromString[Document](items)
      failure("Unexpected success")
    } catch {
      case PayloadError(_, _, message) =>
        expect(message == "input Json document exceeded max arity of `1024`")
    }
  }

  pureTest("throw PayloadError on Document map inserts over maxArity") {
    try {
      val items =
        List.fill(1025)("1").map(i => s""""$i":$i""").mkString("{", ",", "}")
      val _ = readFromString[Document](items)
      failure("Unexpected success")
    } catch {
      case PayloadError(_, _, message) =>
        expect(message == "input Json document exceeded max arity of `1024`")
    }
  }

}
