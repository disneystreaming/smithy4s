/*
 *  Copyright 2021-2023 Disney Streaming
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

package smithy4s.codegen.internals

final class DefaultRenderModeSpec extends munit.FunSuite {

  test("default render mode - NONE") {
    val smithy = """|$version: "2.0"
                    |
                    |metadata smithy4sDefaultRenderMode = "NONE"
                    |
                    |namespace foo
                    |
                    |structure Test {
                    |  one: String
                    |  two: String = "test"
                    |  @required
                    |  three: String
                    |}
                    |""".stripMargin

    val scalaCode =
      """|package foo
         |
         |import smithy4s.Hints
         |import smithy4s.Schema
         |import smithy4s.ShapeId
         |import smithy4s.ShapeTag
         |import smithy4s.schema.Schema.string
         |import smithy4s.schema.Schema.struct
         |
         |final case class Test(one: Option[String], two: String, three: String)
         |
         |object Test extends ShapeTag.Companion[Test] {
         |  val id: ShapeId = ShapeId("foo", "Test")
         |
         |  val hints: Hints = Hints.empty
         |
         |  implicit val schema: Schema[Test] = struct(
         |    string.optional[Test]("one", _.one),
         |    string.required[Test]("two", _.two).addHints(smithy.api.Default(smithy4s.Document.fromString("test"))),
         |    string.required[Test]("three", _.three),
         |  ){
         |    Test.apply
         |  }.withId(id).addHints(hints)
         |}""".stripMargin

    TestUtils.runTest(smithy, scalaCode)
  }

  test("default render mode - OPTION_ONLY") {
    val smithy = """|$version: "2.0"
                    |
                    |metadata smithy4sDefaultRenderMode = "OPTION_ONLY"
                    |
                    |namespace foo
                    |
                    |structure Test {
                    |  one: String
                    |  two: String = "test"
                    |  @required
                    |  three: String
                    |}
                    |""".stripMargin

    val scalaCode =
      """|package foo
         |
         |import smithy4s.Hints
         |import smithy4s.Schema
         |import smithy4s.ShapeId
         |import smithy4s.ShapeTag
         |import smithy4s.schema.Schema.string
         |import smithy4s.schema.Schema.struct
         |
         |final case class Test(two: String, three: String, one: Option[String] = None)
         |
         |object Test extends ShapeTag.Companion[Test] {
         |  val id: ShapeId = ShapeId("foo", "Test")
         |
         |  val hints: Hints = Hints.empty
         |
         |  implicit val schema: Schema[Test] = struct(
         |    string.required[Test]("two", _.two).addHints(smithy.api.Default(smithy4s.Document.fromString("test"))),
         |    string.required[Test]("three", _.three),
         |    string.optional[Test]("one", _.one),
         |  ){
         |    Test.apply
         |  }.withId(id).addHints(hints)
         |}""".stripMargin

    TestUtils.runTest(smithy, scalaCode)
  }

  test("default render mode - FULL") {
    val smithy = """|$version: "2.0"
                    |
                    |metadata smithy4sDefaultRenderMode = "FULL"
                    |
                    |namespace foo
                    |
                    |structure Test {
                    |  one: String
                    |  two: String = "test"
                    |  @required
                    |  three: String
                    |}
                    |""".stripMargin

    val scalaCode =
      """|package foo
         |
         |import smithy4s.Hints
         |import smithy4s.Schema
         |import smithy4s.ShapeId
         |import smithy4s.ShapeTag
         |import smithy4s.schema.Schema.string
         |import smithy4s.schema.Schema.struct
         |
         |final case class Test(three: String, two: String = "test", one: Option[String] = None)
         |
         |object Test extends ShapeTag.Companion[Test] {
         |  val id: ShapeId = ShapeId("foo", "Test")
         |
         |  val hints: Hints = Hints.empty
         |
         |  implicit val schema: Schema[Test] = struct(
         |    string.required[Test]("three", _.three),
         |    string.required[Test]("two", _.two).addHints(smithy.api.Default(smithy4s.Document.fromString("test"))),
         |    string.optional[Test]("one", _.one),
         |  ){
         |    Test.apply
         |  }.withId(id).addHints(hints)
         |}
         |""".stripMargin

    TestUtils.runTest(smithy, scalaCode)
  }

  test("allow for self referencing trait - structure") {
    val smithy = """$version: "2"
                   |
                   |namespace input
                   |
                   |@trait
                   |structure Person {
                   |    @Person
                   |    name: String
                   |}
                   |""".stripMargin

    val scalaCode =
      """package input
        |
        |import smithy4s.Hints
        |import smithy4s.Schema
        |import smithy4s.ShapeId
        |import smithy4s.ShapeTag
        |import smithy4s.schema.Schema.recursive
        |import smithy4s.schema.Schema.string
        |import smithy4s.schema.Schema.struct
        |
        |final case class Person(name: Option[String] = None)
        |
        |object Person extends ShapeTag.Companion[Person] {
        |  val id: ShapeId = ShapeId("input", "Person")
        |
        |  val hints: Hints = Hints(
        |    smithy.api.Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
        |  )
        |
        |  implicit val schema: Schema[Person] = recursive(struct(
        |    string.optional[Person]("name", _.name).addHints(input.Person(name = None)),
        |  ){
        |    Person.apply
        |  }.withId(id).addHints(hints))
        |}
        |""".stripMargin
    TestUtils.runTest(smithy, scalaCode)
  }

  test("allow for self referencing trait - union") {
    val smithy = """$version: "2"
                   |
                   |namespace input
                   |
                   |@trait
                   |union Person {
                   |    @Person(u: "demo")
                   |    name: String
                   |    u: String
                   |}
                   |""".stripMargin
    val scalaCode =
      """package input
        |
        |import smithy4s.Hints
        |import smithy4s.Schema
        |import smithy4s.ShapeId
        |import smithy4s.ShapeTag
        |import smithy4s.schema.Schema.bijection
        |import smithy4s.schema.Schema.recursive
        |import smithy4s.schema.Schema.string
        |import smithy4s.schema.Schema.union
        |
        |sealed trait Person extends scala.Product with scala.Serializable { self =>
        |  @inline final def widen: Person = this
        |  def $ordinal: Int
        |
        |  object project {
        |    def name: Option[String] = Person.NameCase.alt.project.lift(self).map(_.name)
        |    def u: Option[String] = Person.UCase.alt.project.lift(self).map(_.u)
        |  }
        |
        |  def accept[A](visitor: Person.Visitor[A]): A = this match {
        |    case value: Person.NameCase => visitor.name(value.name)
        |    case value: Person.UCase => visitor.u(value.u)
        |  }
        |}
        |object Person extends ShapeTag.Companion[Person] {
        |
        |  def name(name: String): Person = NameCase(name)
        |  def u(u: String): Person = UCase(u)
        |
        |  val id: ShapeId = ShapeId("input", "Person")
        |
        |  val hints: Hints = Hints(
        |    smithy.api.Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
        |  )
        |
        |  final case class NameCase(name: String) extends Person { final def $ordinal: Int = 0 }
        |  final case class UCase(u: String) extends Person { final def $ordinal: Int = 1 }
        |
        |  object NameCase {
        |    val hints: Hints = Hints(
        |      input.Person.UCase("demo").widen,
        |    )
        |    val schema: Schema[Person.NameCase] = bijection(string.addHints(hints), Person.NameCase(_), _.name)
        |    val alt = schema.oneOf[Person]("name")
        |  }
        |  object UCase {
        |    val hints: Hints = Hints.empty
        |    val schema: Schema[Person.UCase] = bijection(string.addHints(hints), Person.UCase(_), _.u)
        |    val alt = schema.oneOf[Person]("u")
        |  }
        |
        |  trait Visitor[A] {
        |    def name(value: String): A
        |    def u(value: String): A
        |  }
        |
        |  object Visitor {
        |    trait Default[A] extends Visitor[A] {
        |      def default: A
        |      def name(value: String): A = default
        |      def u(value: String): A = default
        |    }
        |  }
        |
        |  implicit val schema: Schema[Person] = recursive(union(
        |    Person.NameCase.alt,
        |    Person.UCase.alt,
        |  ){
        |    _.$ordinal
        |  }.withId(id).addHints(hints))
        |}""".stripMargin
    TestUtils.runTest(smithy, scalaCode)
  }
}
