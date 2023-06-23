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

package smithy4s.codegen.internals

final class RendererSpec extends munit.FunSuite {
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
      """string.addHints(smithy.api.Documentation("listFoo"), smithy.api.Deprecated(message = None, since = None))"""
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
      """string.addHints(smithy.api.Documentation("mapFoo"))"""
    val valueSchemaString =
      """int.addHints(smithy.api.Documentation("mapBar"), smithy.api.Deprecated(message = None, since = None))"""
    val requiredString =
      s"""val underlyingSchema: Schema[Map[String, Int]] = map($keySchemaString, $valueSchemaString)"""
    assert(definition.contains(requiredString))
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
}
