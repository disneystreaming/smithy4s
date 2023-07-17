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
        |namespace smithy4s.errors
        |
        |service ErrorService {
        |  version: "1.0.0",
        |  operations: [Operation]
        |}
        |
        |operation Operation {
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
        |namespace smithy4s.errors
        |
        |service ErrorService {
        |  version: "1.0.0",
        |  operations: [Operation]
        |}
        |
        |operation Operation {
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
        |namespace smithy4s.errors
        |
        |service ErrorService {
        |  version: "1.0.0",
        |  operations: [Operation]
        |}
        |
        |operation Operation {
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

    val serviceCode = generateScalaCode(smithy)("smithy4s.errors.ErrorService")

    assertContainsSection(serviceCode, "override val errorable")(
      "override val errorable: Option[Errorable[OperationError]] = Some(this)"
    )
    assertContainsSection(
      serviceCode,
      "val error: UnionSchema[OperationError]"
    )(
      "val error: UnionSchema[OperationError] = OperationError.schema"
    )
    assertContainsSection(serviceCode, "def liftError(throwable: Throwable)")(
      """|def liftError(throwable: Throwable): Option[OperationError] = throwable match {
         |  case e: OperationError => Some(e)
         |  case _ => None
         |}""".stripMargin
    )
    assertContainsSection(serviceCode, "def unliftError(e: OperationError)")(
      "def unliftError(e: OperationError): Throwable = e"
    )

    assertContainsSection(serviceCode, "object OperationError")(
      """|object OperationError {
         |  val id: ShapeId = ShapeId("smithy4s.errors", "OperationError")
         |  val hints: Hints = Hints.empty
         |  val schema: UnionSchema[OperationError] = {
         |    val badRequestAlt = BadRequest.schema.oneOf[OperationError]("BadRequest")
         |    val internalServerErrorAlt = InternalServerError.schema.oneOf[OperationError]("InternalServerError")
         |    union(badRequestAlt, internalServerErrorAlt) {
         |      case _: BadRequest => 0
         |      case _: InternalServerError => 1
         |    }
         |  }
         |}""".stripMargin
    )

  }

  test("Renderer.Config.wildcardArgument = \"?\"") {
    val smithy =
      """
        |$version: "2.0"
        |
        |metadata smithy4sWildcardArgument = "?"
        |
        |namespace smithy4s
        |
        |service Service {
        |  version: "1.0.0",
        |  operations: [Operation]
        |}
        |
        |operation Operation {
        |  input: Unit,
        |  output: Unit,
        |}
        |""".stripMargin

    val serviceCode = generateScalaCode(smithy)("smithy4s.Service")

    assertContainsSection(serviceCode, "val endpoints")(
      """val endpoints: Vector[smithy4s.Endpoint[ServiceOperation, ?, ?, ?, ?, ?]] = Vector(
        |  ServiceOperation.Operation,
        |)""".stripMargin
    )
  }

  test("Renderer.Config.wildcardArgument = default") {
    val smithy =
      """
        |$version: "2.0"
        |
        |namespace smithy4s
        |
        |service Service {
        |  version: "1.0.0",
        |  operations: [Operation]
        |}
        |
        |operation Operation {
        |  input: Unit,
        |  output: Unit,
        |}
        |""".stripMargin

    val serviceCode = generateScalaCode(smithy)("smithy4s.Service")

    assertContainsSection(serviceCode, "val endpoints")(
      """val endpoints: Vector[smithy4s.Endpoint[ServiceOperation, _, _, _, _, _]] = Vector(
        |  ServiceOperation.Operation,
        |)""".stripMargin
    )
  }

  private def testErrorsAsUnionsDisabled(smithy: String) = {
    val serviceCode = generateScalaCode(smithy)("smithy4s.errors.ErrorService")

    assertContainsSection(serviceCode, "override val errorable")(
      "override val errorable: Option[Errorable[OperationError]] = Some(this)"
    )
    assertContainsSection(
      serviceCode,
      "val error: UnionSchema[OperationError]"
    )(
      "val error: UnionSchema[OperationError] = OperationError.schema"
    )
    assertContainsSection(serviceCode, "def liftError(throwable: Throwable)")(
      """|def liftError(throwable: Throwable): Option[OperationError] = throwable match {
         |  case e: BadRequest => Some(OperationError.BadRequestCase(e))
         |  case e: InternalServerError => Some(OperationError.InternalServerErrorCase(e))
         |  case _ => None
         |}""".stripMargin
    )
    assertContainsSection(serviceCode, "def unliftError(e: OperationError)")(
      """|def unliftError(e: OperationError): Throwable = e match {
         |  case OperationError.BadRequestCase(e) => e
         |  case OperationError.InternalServerErrorCase(e) => e
         |}""".stripMargin
    )

    println(serviceCode)
    assertContainsSection(serviceCode, "sealed trait OperationError")(
      """|sealed trait OperationError extends scala.Product with scala.Serializable {
         |  @inline final def widen: OperationError = this
         |  def _ordinal: Int
         |}""".stripMargin
    )

    assertContainsSection(serviceCode, "object OperationError")(
      """|object OperationError extends ShapeTag.Companion[OperationError] {
         |  val id: ShapeId = ShapeId("smithy4s.errors", "OperationError")
         |  val hints: Hints = Hints.empty
         |  final case class BadRequestCase(badRequest: BadRequest) extends OperationError { final def _ordinal: Int = 0 }
         |  final case class InternalServerErrorCase(internalServerError: InternalServerError) extends OperationError { final def _ordinal: Int = 1 }
         |  object BadRequestCase {
         |    val hints: Hints = Hints.empty
         |    val schema: Schema[BadRequestCase] = bijection(BadRequest.schema.addHints(hints), BadRequestCase(_), _.badRequest)
         |    val alt = schema.oneOf[OperationError]("BadRequest")
         |  }
         |  object InternalServerErrorCase {
         |    val hints: Hints = Hints.empty
         |    val schema: Schema[InternalServerErrorCase] = bijection(InternalServerError.schema.addHints(hints), InternalServerErrorCase(_), _.internalServerError)
         |    val alt = schema.oneOf[OperationError]("InternalServerError")
         |  }
         |  implicit val schema: UnionSchema[OperationError] = union(
         |    BadRequestCase.alt,
         |    InternalServerErrorCase.alt,
         |  ){
         |    _._ordinal
         |  }
         |}""".stripMargin
    )
  }
}
