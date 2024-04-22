/*
 *  Copyright 2021-2024 Disney Streaming
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

import org.scalacheck.Gen
import org.scalacheck.Prop
import software.amazon.smithy.model.shapes.EnumShape
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.StructureShape

final class RendererSpec extends munit.ScalaCheckSuite {
  import TestUtils._

  test("list member hints should be preserved") {
    val smithy =
      """
        |$version: "2.0"
        |
        |namespace smithy4s
        |
        |list ListWithMemberHints {
        |  @documentation("listFoo")
        |  @deprecated
        |  member: String
        |}
        |""".stripMargin

    val contents = generateScalaCode(smithy).values
    val definition =
      contents.find(_.contains("object ListWithMemberHints")) match {
        case None =>
          fail(
            "No generated scala file contains valid ListWithMemberHints definition"
          )
        case Some(code) => code
      }

    val memberSchemaString =
      """string.addMemberHints(smithy.api.Documentation("listFoo"), smithy.api.Deprecated(message = None, since = None))"""
    val requiredString =
      s"""val underlyingSchema: Schema[List[String]] = list($memberSchemaString)"""
    assert(definition.contains(requiredString))
  }

  test("map member hints should be preserved") {
    val smithy =
      """
        |$version: "2.0"
        |
        |namespace smithy4s
        |
        |map MapWithMemberHints {
        |  @documentation("mapFoo")
        |  key: String
        |
        |  @documentation("mapBar")
        |  @deprecated
        |  value: Integer
        |}
        |""".stripMargin

    val contents = generateScalaCode(smithy).values
    val definition =
      contents.find(_.contains("object MapWithMemberHints")) match {
        case None =>
          fail(
            "No generated scala file contains valid MapWithMemberHints definition"
          )
        case Some(code) => code
      }

    val keySchemaString =
      """string.addMemberHints(smithy.api.Documentation("mapFoo"))"""
    val valueSchemaString =
      """int.addMemberHints(smithy.api.Documentation("mapBar"), smithy.api.Deprecated(message = None, since = None))"""
    val requiredString =
      s"""val underlyingSchema: Schema[Map[String, Int]] = map($keySchemaString, $valueSchemaString)"""
    assert(definition.contains(requiredString))
  }

  test("enum trait hints should be preserved") {
    val smithy = s"""
                    |$$version: "2.0"
                    |namespace smithy4s
                    |
                    |@documentation("this is an enum Suit")
                    |@enum([
                    |{
                    |  value: "DIAMOND",
                    |  name: "DIAMOND",
                    |  documentation: "this is a DIAMOND"
                    |},
                    |{
                    |  value: "HAERT",
                    |  name: "HAERT",
                    |  documentation: "this is a HAERT",
                    |  deprecated: true
                    |},
                    |{
                    |  value: "HEART",
                    |  name: "HEART",
                    |  documentation: "this is a HEART"
                    |},
                    |{
                    |  value: "CLUB",
                    |  name: "CLUB"
                    |},
                    |{
                    |  value: "SPADE"
                    |  name: "SPADE"
                    |}
                    |])
                    |string Suit
                    |
                    |""".stripMargin
    val contents = generateScalaCode(smithy).values
    val definition =
      contents.find(content =>
        content.contains("sealed abstract class Suit") && content.contains(
          "object Suit extends Enumeration[Suit]"
        )
      ) match {
        case None =>
          fail(
            "enum must be rendered as a sealed abstract class and its companion object"
          )
        case Some(code) => code
      }
    val classDoc = """/** this is an enum Suit
                     |  * @param DIAMOND
                     |  *   this is a DIAMOND
                     |  * @param HAERT
                     |  *   this is a HAERT
                     |  * @param HEART
                     |  *   this is a HEART
                     |  */""".stripMargin
    val diamondWithDocRendered =
      List(
        """case object DIAMOND extends Suit("DIAMOND", "DIAMOND", 0, Hints.empty)""",
        """override val hints: Hints = Hints(smithy.api.Documentation("this is a DIAMOND")).lazily"""
      )
    // NOTE: enum trait and enum shape are not 100% compatible. For example, enum trait doesn't support deprecated$since and deprecated$message.
    val haertWithDeprecationAndDocRendered = List(
      "/** this is a HAERT */",
      "@deprecated",
      """case object HAERT extends Suit("HAERT", "HAERT", 1, Hints.empty)""",
      """override val hints: Hints = Hints(smithy.api.Documentation("this is a HAERT"), smithy.api.Deprecated(message = None, since = None)).lazily"""
    )

    assert(
      definition.contains(classDoc),
      "generated class must hold documentation"
    )
    assert(
      diamondWithDocRendered.forall(definition.contains),
      "DIAMOND variant must hold hints from `@enum` trait"
    )
    assert(
      haertWithDeprecationAndDocRendered.forall(definition.contains),
      "variants generated from `@enum` trait must hold hints, but deprecation hint cannot support message and since properties."
    )
    assert(
      !definition.contains("smithy.api.Enum"),
      "smithy.api.Enum must be discarded"
    )

  }

  test("unnamed enum trait can be rendered as enum") {
    val smithy = """
                   |
                   |$version: "2.0"
                   |
                   |namespace smithy4s
                   |
                   |@enum([
                   |{
                   |  value: "HEAD"
                   |},
                   |{
                   |  value: "t:a$i\\l"
                   |}
                   |])
                   |string Coin
                   |""".stripMargin
    val contents = generateScalaCode(smithy).values
    val definition =
      contents.find(content =>
        content.contains("sealed abstract class Coin") && content.contains(
          "object Coin extends Enumeration[Coin]"
        )
      ) match {
        case None =>
          fail(
            "enum must be rendered as a sealed abstract class and its companion object"
          )
        case Some(code) => code
      }
    assert(
      definition.contains(
        """case object HEAD extends Coin("HEAD", "HEAD", 0, Hints.empty)"""
      ),
      "enum trait value without name must be rendered as enum variant"
    )
    assert(
      definition.contains(
        """case object TAIL extends Coin("t:a$i\l", "TAIL", 1, Hints.empty)"""
      ),
      "enum trait value without name but with non alphanumeric value must be rendered as enum variant"
    )
  }

  test("enum hints should be preserved") {
    val smithy = """
                   |$version: "2.0"
                   |
                   |namespace smithy4s
                   |
                   |@documentation("this is an enum Suit")
                   |enum Suit {
                   |  @documentation("this is a DIAMOND")
                   |  DIAMOND
                   |  /// this is a HAERT
                   |  @deprecated(since:"0.0.0", message: "typo")
                   |  HAERT
                   |  /// this is a HEART
                   |  HEART
                   |  CLUB
                   |  SPADE
                   |}
                   |
                   |""".stripMargin
    val contents = generateScalaCode(smithy).values
    val definition =
      contents.find(content =>
        content.contains("sealed abstract class Suit") && content.contains(
          "object Suit extends Enumeration[Suit]"
        )
      ) match {
        case None =>
          fail(
            "enum must be rendered as a sealed abstract class and its companion object"
          )
        case Some(code) => code
      }
    val classDoc = """/** this is an enum Suit
                     |  * @param DIAMOND
                     |  *   this is a DIAMOND
                     |  * @param HAERT
                     |  *   this is a HAERT
                     |  * @param HEART
                     |  *   this is a HEART
                     |  */""".stripMargin
    val diamondWithDocRendered =
      """  case object DIAMOND extends Suit("DIAMOND", "DIAMOND", 0, Hints.empty) {
        |    override val hints: Hints = Hints(smithy.api.Documentation("this is a DIAMOND")).lazily
        |  }""".stripMargin
    val haertWithDeprecationAndDocRendered =
      """|  /** this is a HAERT */
         |  @deprecated(message = "typo", since = "0.0.0")
         |  case object HAERT extends Suit("HAERT", "HAERT", 1, Hints.empty) {
         |    override val hints: Hints = Hints(smithy.api.Documentation("this is a HAERT"), smithy.api.Deprecated(message = Some("typo"), since = Some("0.0.0"))).lazily
         |  }""".stripMargin

    assert(
      definition.contains(classDoc),
      "generated class must hold documentation for itself and variants with doc hint."
    )
    assert(
      definition.contains(diamondWithDocRendered),
      "DIAMOND variant must hold hints from `@enum` trait"
    )
    assert(
      definition.contains(haertWithDeprecationAndDocRendered),
      "variants generated from enum shape must hold hints and deprecation hint must keep message and since properties."
    )
  }

  test("structure must generate final case class") {
    val smithy =
      """
        |$version: "2.0"
        |
        |namespace smithy4s
        |
        |structure GetObjectInput {
        |  key: String
        |}
        |""".stripMargin

    val contents = generateScalaCode(smithy).values
    val requiredString =
      "final case class GetObjectInput(key: Option[String] = None)"
    val caseClass = contents.find(_.contains(requiredString))

    assert(caseClass.isDefined)
  }

  test("union should generate final case classes") {
    val smithy =
      """
        |$version: "2.0"
        |
        |namespace smithy4s
        |
        |union Foo {
        | int: Integer,
        | str: String
        |}
        |""".stripMargin

    val contents = generateScalaCode(smithy).values
    val requiredIntCase = "final case class IntCase(int: Int)"
    val requiredStrCase = "final case class StrCase(str: String)"

    assert(contents.exists(_.contains(requiredIntCase)))
    assert(contents.exists(_.contains(requiredStrCase)))
  }

  test("unspecified members of deprecated trait are rendered as N/A") {
    val smithy =
      """
        |$version: "2.0"
        |
        |namespace smithy4s
        |
        |@deprecated
        |integer MyInt
        |
        |@deprecated(message: "msg")
        |string MyString
        |
        |@deprecated(since: "0.0.0")
        |boolean MyBool
        |""".stripMargin

    val contents = generateScalaCode(smithy).values
    assert(
      contents.exists(
        _.contains("""@deprecated(message = "N/A", since = "N/A")""")
      )
    )
    assert(
      contents.exists(
        _.contains("""@deprecated(message = "msg", since = "N/A")""")
      )
    )
    assert(
      contents.exists(
        _.contains("""@deprecated(message = "N/A", since = "0.0.0")""")
      )
    )
  }
  test(
    "service annotated with @generateServiceProduct also generates a service product"
  ) {
    val smithy =
      """
        |$version: "2.0"
        |
        |namespace smithy4s
        |
        |use smithy4s.meta#generateServiceProduct
        |
        |@generateServiceProduct
        |service MyService {
        |  version: "1.0.0"
        |}
        |""".stripMargin

    val contents = generateScalaCode(smithy).values

    List(
      "trait MyServiceProductGen",
      "object MyServiceProductGen extends ServiceProduct[MyServiceProductGen]",
      "object MyServiceGen extends Service.Mixin[MyServiceGen, MyServiceOperation] with ServiceProduct.Mirror[MyServiceGen]",
      "  type Prod[F[_, _, _, _, _]] = MyServiceProductGen[F]\n" +
        "  val serviceProduct: ServiceProduct.Aux[MyServiceProductGen, MyServiceGen] = MyServiceProductGen"
    ).foreach(str => assert(contents.exists(_.contains(str))))
  }
  test(
    "service not annotated with @generateServiceProduct doesn't generate any extra code"
  ) {
    val smithy = """
                   |$version: "2.0"
                   |
                   |namespace smithy4s
                   |
                   |service MyService {
                   |  version: "1.0.0"
                   |}
                   |""".stripMargin

    val contents = generateScalaCode(smithy).values

    assert(!contents.exists(_.contains("ServiceProduct")))
  }
  test(
    "string literal not containing $ is rendered as an uninterpolated string"
  ) {
    val smithy = """
                   |$version: "2.0"
                   |
                   |namespace smithy4s
                   |
                   |@documentation("foo bar")
                   |string MyString
                   |""".stripMargin

    val contents = generateScalaCode(smithy).values

    assert(contents.exists(_.contains("smithy.api.Documentation(\"foo bar\")")))
  }
  test(
    "string literal containing $ is rendered as an interpolated string with $ escaped as $$"
  ) {
    val smithy = """
                   |$version: "2.0"
                   |
                   |namespace smithy4s
                   |
                   |@documentation("foo $bar")
                   |string MyString
                   |""".stripMargin

    val contents = generateScalaCode(smithy).values

    assert(
      contents.exists(_.contains("smithy.api.Documentation(s\"foo $$bar\")"))
    )
  }

  test(
    "string literal containing /* is rendered as a string with * escaped as &ast;"
  ) {
    val smithy = """
                   |$version: "2.0"
                   |
                   |namespace smithy4s
                   |
                   |@documentation("/*")
                   |string MyString
                   |""".stripMargin

    val contents = generateScalaCode(smithy).values

    assert(contents.exists(_.contains("/** /&ast; */")))
    assert(contents.exists(_.contains("""smithy.api.Documentation("/*")""")))
  }

  test("trait with subcomponent targeting smithy.api#Unit") {
    val smithy =
      """
        |$version: "2"
        |
        |namespace input
        |
        |@trait
        |union myTrait {
        |    u: Unit
        |}
        |
        |@myTrait(u: {
        |
        |})
        |string Example
        |""".stripMargin

    val contents = generateScalaCode(smithy).values

    assert(
      contents.exists(
        _.contains(
          "sealed trait MyTrait extends scala.Product with scala.Serializable"
        )
      )
    )
  }

  property("enumeration order is preserved") {

    // custom input to avoid scalacheck shrinking
    case class Input(identifiers: List[String])
    Prop.forAll(
      Gen
        .nonEmptyListOf(Gen.identifier.map(_.toUpperCase()))
        .map(_.distinct)
        .map(Input(_))
    ) { input =>
      import input.identifiers
      val builder = EnumShape.builder().id("input#MyEnum")
      identifiers.foreach(id => builder.addMember(id, id))
      val enumShape = builder.build()
      val unitShape = StructureShape.builder().id("smithy.api#Unit").build()

      val model = Model.builder().addShapes(unitShape, enumShape).build()

      val allContents = generateScalaCode(model)
      allContents.values.exists { fileContent =>
        val cleanLines = fileContent.linesIterator
          .map(_.trim().replace(",", "")) // removing whitespace and commas
          .toList

        cleanLines.containsSlice(identifiers)
      }
    }
  }

}
