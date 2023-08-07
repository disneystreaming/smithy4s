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

package smithy4s.decline.core

import cats.implicits._
import cats.kernel.Eq
import com.monovore.decline.{Command, Opts}
import com.monovore.decline.Help
import smithy.api.Length
import smithy.api.TimestampFormat
import smithy4s.Document
import smithy4s.Hints
import smithy4s.Timestamp
import smithy4s.schema.EnumValue
import smithy4s.schema.Schema
import smithy4s.schema.Schema._
import weaver._
import smithy4s.example.{OpenEnumTest, OpenIntEnumTest}

object OptsSchematicSpec extends SimpleIOSuite {
  def sampleStruct[A](name: String, schema: Schema[A]): Schema[A] =
    struct(
      schema.required[A](name, identity)
    )(identity)

  def parseOpts[A](schema: Schema[A])(input: String*): Either[Help, A] =
    Command("test-command", "test-header")(schema.compile[Opts](OptsVisitor))
      .parse(input)

  val sampleUnion = sampleStruct(
    "example", {
      Schema.either(sampleStruct("int", int), sampleStruct("str", string))
    }
  )

  implicit val openEnumTestEq: Eq[OpenEnumTest] = Eq.fromUniversalEquals
  implicit val openIntEnumTestEq: Eq[OpenIntEnumTest] = Eq.fromUniversalEquals

  sealed trait Superpower
  case object Fire extends Superpower
  case object Ice extends Superpower
  case object Water extends Superpower

  object Superpower {

    implicit val eq: Eq[Superpower] = Eq.fromUniversalEquals

    val schema: Schema[Superpower] = {

      val fire: EnumValue[Superpower] =
        EnumValue("Fire", 0, Fire, "FIRE", Hints.empty)
      val ice: EnumValue[Superpower] =
        EnumValue("Ice", 1, Ice, "ICE", Hints.empty)
      val water: EnumValue[Superpower] =
        EnumValue("Water", 2, Water, "WATER", Hints.empty)

      sampleStruct(
        "superpower",
        stringEnumeration[Superpower](
          {
            case Fire  => fire
            case Ice   => ice
            case Water => water
          },
          List(
            fire,
            ice,
            water
          )
        )
      )
    }

  }

  case class Person(name: String, age: Int)

  object Person {
    implicit val eq: Eq[Person] = Eq.fromUniversalEquals

    val schema: Schema[Person] =
      struct(
        string.required[Person]("name", _.name),
        int.required[Person]("age", _.age)
      )(Person.apply)

  }

  case class PersonOptional(name: String, age: Option[Int])

  object PersonOptional {
    implicit val eq: Eq[PersonOptional] = Eq.fromUniversalEquals

    val schema: Schema[PersonOptional] =
      struct(
        string.required[PersonOptional]("name", _.name),
        int.optional[PersonOptional]("age", _.age)
      )(PersonOptional.apply)

  }

  case class FullyOptional(value1: Option[Double], value2: Option[String])

  object FullyOptional {
    implicit val eq: Eq[FullyOptional] = Eq.fromUniversalEquals

    val schema: Schema[FullyOptional] =
      struct(
        double.optional[FullyOptional]("value1", _.value1),
        string.optional[FullyOptional]("value2", _.value2)
      )(FullyOptional.apply)

  }

  // generated from smithy4s codegen
  case class Recursive(name: String, parent: Option[Recursive] = None)

  object Recursive extends smithy4s.ShapeTag.Companion[Recursive] {
    val id: smithy4s.ShapeId =
      smithy4s.ShapeId("smithy4s.decline.test", "Recursive")

    val schema: smithy4s.Schema[Recursive] = recursive(
      struct(
        string
          .required[Recursive]("name", _.name)
          .addHints(smithy.api.Required()),
        Recursive.schema.optional[Recursive]("parent", _.parent)
      ) {
        Recursive.apply
      }.addHints(id)
    )

  }

  implicit val req: Eq[Recursive] = Eq.fromUniversalEquals

  implicit val tsEq: Eq[Timestamp] = Eq.fromUniversalEquals

  def timestampTest(schema: Schema[Timestamp], input: String) = assert.parsed(
    parseOpts(sampleStruct("ts", schema))(
      input
    ),
    Timestamp.apply(2020, 1, 1, 0, 0, 0, 0)
  )

  implicit class ParsesAsSyntax(expect: Expect) {

    implicit val eqHelp: Eq[Help] = Eq.fromUniversalEquals

    implicit val eqThrowable: Eq[Throwable] = Eq.fromUniversalEquals

    def parsed[A: Eq](
        actual: Either[Help, A],
        expected: A
    ): Expectations = expect.eql(actual, Right(expected))

    def failureSubstring(
        actual: Either[Help, Any],
        expected: String
    )(implicit
        loc: SourceLocation
    ): Expectations = actual.fold(
      result => assert(result.toString().contains(expected)),
      fail("Expected failure, but got success 😩")
    )
  }

  pureTest("compile unit") {
    assert.parsed(
      parseOpts(unit)(),
      ()
    )
  }

  pureTest("compile unit with bijection (const)") {
    assert.parsed(
      parseOpts(bijection[Unit, Int](unit, (_: Unit) => 42, (_: Int) => ()))(),
      42
    )
  }

  pureTest("compile string field") {
    assert.parsed(
      parseOpts(sampleStruct("elem", string))("example"),
      "example"
    )
  }

  pureTest("compile int field") {
    assert.parsed(
      parseOpts(sampleStruct("elem", int))("35"),
      35
    )
  }

  pureTest("compile int field with bijection") {
    assert.parsed(
      parseOpts(
        bijection[Int, String](
          sampleStruct("elem", int),
          (i: Int) => i.toString + ".asString",
          (str: String) => sys.error("impossible")
        )
      )("42"),
      "42.asString"
    )
  }

  pureTest("compile double field") {
    assert.parsed(
      parseOpts(sampleStruct("elem", double))("35.1"),
      35.1d
    )
  }

  pureTest("compile flag") {
    assert.parsed(
      parseOpts(sampleStruct("condition", boolean))("--condition"),
      true
    )
  }

  pureTest("compile boolean list") {
    assert.parsed(
      parseOpts(
        sampleStruct("condition", list(boolean))
      )("true", "false", "true"),
      List(true, false, true)
    )
  }

  pureTest("compile missing flag") {
    assert.parsed(
      parseOpts(sampleStruct("condition", boolean))(),
      false
    )
  }

  pureTest("compile union - first match") {

    assert.parsed[Either[Int, String]](
      parseOpts(sampleUnion)("--int", "35"),
      35.asLeft
    )
  }

  pureTest("compile union - second match") {
    assert.parsed[Either[Int, String]](
      parseOpts(sampleUnion)("--str", "a42"),
      "a42".asRight
    )
  }

  pureTest("compile enum - matching") {
    assert.parsed(
      parseOpts(Superpower.schema)("Fire"),
      Fire
    )
  }

  pureTest("compile open string enum - known value") {
    assert.parsed(
      parseOpts(sampleStruct("test", OpenEnumTest.schema))("ONE"),
      OpenEnumTest.ONE
    )
  }

  pureTest("compile open string enum - unknown value") {
    assert.parsed(
      parseOpts(sampleStruct("test", OpenEnumTest.schema))("SOMETHING"),
      OpenEnumTest.Unknown("SOMETHING")
    )
  }

  pureTest("compile open int enum - known value") {
    assert.parsed(
      parseOpts(sampleStruct("test", OpenIntEnumTest.schema))("1"),
      OpenIntEnumTest.ONE
    )
  }

  pureTest("compile open int enum - unknown value") {
    assert.parsed(
      parseOpts(sampleStruct("test", OpenIntEnumTest.schema))("123"),
      OpenIntEnumTest.Unknown(123)
    )
  }

  pureTest("compile enum - not matching") {
    assert.failureSubstring(
      parseOpts(Superpower.schema)("Wind"),
      """Unknown value "Wind" for input superpower. Allowed values: Fire, Ice, Water"""
    )
  }

  pureTest("struct with required fields") {
    assert.parsed(
      parseOpts(Person.schema)("Mary", "35"),
      Person("Mary", 35)
    )
  }

  pureTest("struct with optional field") {
    assert.parsed(
      parseOpts(PersonOptional.schema)("Mary"),
      PersonOptional("Mary", None)
    )
  }

  pureTest("nested struct with required fields") {
    assert.parsed(
      parseOpts(sampleStruct("input", Person.schema))(
        "--name",
        "Mary",
        "--age",
        "35"
      ),
      Person("Mary", 35)
    )
  }

  pureTest("deeply nested struct") {
    assert.parsed(
      parseOpts(
        sampleStruct(
          "input3",
          sampleStruct(
            "input2",
            sampleStruct(
              "input1",
              string
            )
          )
        )
      )("--input1", "hello"),
      "hello"
    )
  }

  pureTest("nested struct with optional field") {
    assert.parsed(
      parseOpts(sampleStruct("input", PersonOptional.schema))("--name", "Mary"),
      PersonOptional("Mary", None)
    )
  }

  pureTest("nested struct with all optional fields - no arguments") {
    assert.parsed(
      parseOpts(sampleStruct("input", FullyOptional.schema))(),
      FullyOptional(None, None)
    )
  }

  pureTest("nested struct with all optional fields - all arguments") {
    assert.parsed(
      parseOpts(sampleStruct("input", FullyOptional.schema))(
        "--value1",
        "1.1",
        "--value2",
        "a"
      ),
      FullyOptional(Some(1.1d), Some("a"))
    )
  }

  pureTest("nested struct with all optional fields - first argument only") {
    assert.parsed(
      parseOpts(sampleStruct("input", FullyOptional.schema))("--value1", "1.1"),
      FullyOptional(Some(1.1d), None)
    )
  }

  pureTest("nested struct with all optional fields - second argument only") {
    assert.parsed(
      parseOpts(sampleStruct("input", FullyOptional.schema))("--value2", "a"),
      FullyOptional(None, Some("a"))
    )
  }

  pureTest("timestamp with no format hint") {
    timestampTest(
      timestamp,
      "1577836800"
    )
  }

  pureTest("timestamp with DATE_TIME format hint") {
    timestampTest(
      timestamp.addHints(TimestampFormat.DATE_TIME.widen),
      "2020-01-01T00:00:00.000Z"
    )
  }

  pureTest("timestamp with EPOCH_SECONDS format hint") {
    timestampTest(
      timestamp.addHints(TimestampFormat.EPOCH_SECONDS.widen),
      "1577836800"
    )
  }

  pureTest("timestamp with HTTP_DATE format hint") {
    timestampTest(
      timestamp.addHints(TimestampFormat.HTTP_DATE.widen),
      "Wed, 01 Jan 2020 00:00:00 GMT"
    )
  }

  pureTest("compile document") {
    implicit val docEq: Eq[Document] = Eq.fromUniversalEquals

    assert.parsed(
      parseOpts(sampleStruct("document", document))(
        """{ "foo": "bar", "baz": ["a", "b"] }"""
      ),
      Document.obj(
        "foo" -> Document.fromString("bar"),
        "baz" -> Document.array(
          Document.fromString("a"),
          Document.fromString("b")
        )
      )
    )
  }

  pureTest("compile list of strings") {
    assert.parsed(
      parseOpts(sampleStruct("list", list(string)))("a", "b", "a"),
      List("a", "b", "a")
    )
  }

  pureTest("compile nested list of strings") {
    assert.parsed(
      parseOpts(sampleStruct("root", sampleStruct("items", list(string))))(
        "--items",
        "a",
        "--items",
        "b",
        "--items",
        "a"
      ),
      List("a", "b", "a")
    )
  }

  pureTest("compile set of strings") {
    assert.parsed(
      parseOpts(sampleStruct("set", set(string)))("a", "b", "a"),
      Set("a", "b")
    )
  }

  pureTest("compile recursive type") {

    assert.parsed(
      parseOpts(sampleStruct("recursive", Recursive.schema))(
        """{"name": "foo", "parent": {"name": "bar"}}"""
      ),
      Recursive("foo", Some(Recursive("bar", None)))
    )
  }

  pureTest("compile list of recursive type") {

    assert.parsed(
      parseOpts(sampleStruct("recursive", list(Recursive.schema)))(
        """{"name": "foo", "parent": {"name": "bar"}}""",
        """{"name":"baz"}"""
      ),
      Recursive("foo", Some(Recursive("bar", None))) ::
        Recursive("baz") ::
        Nil
    )
  }

  pureTest("compile nested list of recursive type") {

    assert.parsed(
      parseOpts(
        sampleStruct("recursive", sampleStruct("items", list(Recursive.schema)))
      )(
        "--items",
        """{"name": "foo", "parent": {"name": "bar"}}""",
        "--items",
        """{"name":"baz"}"""
      ),
      Recursive("foo", Some(Recursive("bar", None))) ::
        Recursive("baz") ::
        Nil
    )
  }

  pureTest("compile surjection - success") {
    assert.parsed(
      parseOpts(
        sampleStruct(
          "surjection",
          string.validated[Length](Length(min = Some(1)))
        )
      )("a"),
      "a"
    )
  }

  pureTest("compile surjection - failure") {
    assert.failureSubstring(
      parseOpts(
        sampleStruct(
          "surjection",
          string.validated[Length](Length(min = Some(1)))
        )
      )(""),
      "length required to be >= 1, but was 0"
    )
  }

  pureTest("compile surjection in list - success") {
    assert.parsed(
      parseOpts(
        sampleStruct(
          "surjection",
          list(string.validated[Length](Length(min = Some(1))))
        )
      )("a", "b", "c"),
      List("a", "b", "c")
    )
  }

  pureTest("compile surjection in list - failure in one of the items") {
    assert.failureSubstring(
      parseOpts(
        sampleStruct(
          "surjection",
          list(string.validated[Length](Length(min = Some(1))))
        )
      )("a", "b", ""),
      "length required to be >= 1, but was 0"
    )
  }

  pureTest("compile map") {
    assert.parsed(
      parseOpts(sampleStruct("map", map(string, int)))("""{"k": 42}"""),
      Map("k" -> 42)
    )
  }

}
