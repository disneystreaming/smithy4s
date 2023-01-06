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

final class RendererConfigSpec extends munit.FunSuite {
  import TestUtils._

  test("Renderer.Config.errorsAsScala3Unions = default") {
    val smithy =
      """
        |$version: "2.0"
        |
        |namespace hello.world
        |
        |service HelloWorldService {
        |  version: "1.0.0",
        |  operations: [Hello]
        |}
        |
        |operation Hello {
        |  input: Unit,
        |  output: Unit,
        |  errors: [BadRequest, InternalServerError]
        |}
        |
        |@error("client")
        |structure BadRequest {
        |  @required
        |  reason: String
        |}
        |
        |@error("server")
        |structure InternalServerError {
        |  @required
        |  stackTrace: String
        |}
        |""".stripMargin

    testErrorsAsUnionsDisabled(smithy)
  }

  test("Renderer.Config.errorsAsScala3Unions = false") {
    val smithy =
      """
        |$version: "2.0"
        |
        |metadata smithy4sErrorsAsScala3Unions = false
        |
        |namespace hello.world
        |
        |service HelloWorldService {
        |  version: "1.0.0",
        |  operations: [Hello]
        |}
        |
        |operation Hello {
        |  input: Unit,
        |  output: Unit,
        |  errors: [BadRequest, InternalServerError]
        |}
        |
        |@error("client")
        |structure BadRequest {
        |  @required
        |  reason: String
        |}
        |
        |@error("server")
        |structure InternalServerError {
        |  @required
        |  stackTrace: String
        |}
        |""".stripMargin

    testErrorsAsUnionsDisabled(smithy)
  }

  test("Renderer.Config.errorsAsScala3Unions = true") {
    val smithy =
      """
        |$version: "2.0"
        |
        |metadata smithy4sErrorsAsScala3Unions = true
        |
        |namespace hello.world
        |
        |service HelloWorldService {
        |  version: "1.0.0",
        |  operations: [Hello]
        |}
        |
        |operation Hello {
        |  input: Unit,
        |  output: Unit,
        |  errors: [BadRequest, InternalServerError]
        |}
        |
        |@error("client")
        |structure BadRequest {
        |  @required
        |  reason: String
        |}
        |
        |@error("server")
        |structure InternalServerError {
        |  @required
        |  stackTrace: String
        |}
        |""".stripMargin

    val contents = generateScalaCode(smithy)
    val serviceCode = findServiceCode(contents)

    assertContainsSection(serviceCode, "override val errorable")(
      "override val errorable: Option[Errorable[HelloError]] = Some(this)"
    )
    assertContainsSection(serviceCode, "val error: UnionSchema[HelloError]")(
      "val error: UnionSchema[HelloError] = HelloError.schema"
    )
    assertContainsSection(serviceCode, "def liftError(throwable: Throwable)")(
      """|def liftError(throwable: Throwable): Option[HelloError] = throwable match {
         |  case e: HelloError => Some(e)
         |  case _ => None
         |}""".stripMargin
    )
    assertContainsSection(serviceCode, "def unliftError(e: HelloError)")(
      "def unliftError(e: HelloError): Throwable = e"
    )

    assertContainsSection(serviceCode, "object HelloError")(
      """|object HelloError {
         |  val id: ShapeId = ShapeId("hello.world", "HelloError")
         |  val hints: Hints = Hints.empty
         |  val schema: UnionSchema[HelloError] = {
         |    val badRequestAlt = BadRequest.schema.oneOf[HelloError]("BadRequest")
         |    val internalServerErrorAlt = InternalServerError.schema.oneOf[HelloError]("InternalServerError")
         |    union(badRequestAlt, internalServerErrorAlt) {
         |      case c: BadRequest => badRequestAlt(c)
         |      case c: InternalServerError => internalServerErrorAlt(c)
         |    }
         |  }
         |}""".stripMargin
    )

  }

  private def findServiceCode(contents: List[String]): String = {
    contents.find(_.contains("trait HelloWorldServiceGen")) match {
      case None =>
        fail("No generated scala file contains valid service definition")
      case Some(svc) => svc
    }
  }

  private def testErrorsAsUnionsDisabled(smithy: String) = {
    val contents = generateScalaCode(smithy)
    val serviceCode = findServiceCode(contents)

    assertContainsSection(serviceCode, "override val errorable")(
      "override val errorable: Option[Errorable[HelloError]] = Some(this)"
    )
    assertContainsSection(serviceCode, "val error: UnionSchema[HelloError]")(
      "val error: UnionSchema[HelloError] = HelloError.schema"
    )
    assertContainsSection(serviceCode, "def liftError(throwable: Throwable)")(
      """|def liftError(throwable: Throwable): Option[HelloError] = throwable match {
         |  case e: BadRequest => Some(HelloError.BadRequestCase(e))
         |  case e: InternalServerError => Some(HelloError.InternalServerErrorCase(e))
         |  case _ => None
         |}""".stripMargin
    )
    assertContainsSection(serviceCode, "def unliftError(e: HelloError)")(
      """|def unliftError(e: HelloError): Throwable = e match {
         |  case HelloError.BadRequestCase(e) => e
         |  case HelloError.InternalServerErrorCase(e) => e
         |}""".stripMargin
    )

    assertContainsSection(serviceCode, "sealed trait HelloError")(
      """|sealed trait HelloError extends scala.Product with scala.Serializable {
         |  @inline final def widen: HelloError = this
         |}""".stripMargin
    )

    assertContainsSection(serviceCode, "object HelloError")(
      """|object HelloError extends ShapeTag.Companion[HelloError] {
         |  val id: ShapeId = ShapeId("hello.world", "HelloError")
         |  val hints: Hints = Hints.empty
         |  case class BadRequestCase(badRequest: BadRequest) extends HelloError
         |  case class InternalServerErrorCase(internalServerError: InternalServerError) extends HelloError
         |  object BadRequestCase {
         |    val hints: Hints = Hints.empty
         |    val schema: Schema[BadRequestCase] = bijection(BadRequest.schema.addHints(hints), BadRequestCase(_), _.badRequest)
         |    val alt = schema.oneOf[HelloError]("BadRequest")
         |  }
         |  object InternalServerErrorCase {
         |    val hints: Hints = Hints.empty
         |    val schema: Schema[InternalServerErrorCase] = bijection(InternalServerError.schema.addHints(hints), InternalServerErrorCase(_), _.internalServerError)
         |    val alt = schema.oneOf[HelloError]("InternalServerError")
         |  }
         |  implicit val schema: UnionSchema[HelloError] = union(
         |    BadRequestCase.alt,
         |    InternalServerErrorCase.alt,
         |  ){
         |    case c: BadRequestCase => BadRequestCase.alt(c)
         |    case c: InternalServerErrorCase => InternalServerErrorCase.alt(c)
         |  }
         |}""".stripMargin
    )
  }
}
