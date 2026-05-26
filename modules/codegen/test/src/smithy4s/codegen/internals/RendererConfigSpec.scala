/*
 *  Copyright 2021-2026 Disney Streaming
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
      """|object OperationError extends ErrorSchema.Companion[OperationError] {
         |  val id: ShapeId = ShapeId("smithy4s.errors", "OperationError")
         |  val hints: Hints = Hints.empty
         |  val schema: Schema[OperationError] = {
         |    val badRequestAlt = BadRequest.schema.oneOf[OperationError]("BadRequest")
         |    val internalServerErrorAlt = InternalServerError.schema.oneOf[OperationError]("InternalServerError")
         |    union[OperationError](badRequestAlt, internalServerErrorAlt) {
         |      case _: BadRequest => 0
         |      case _: InternalServerError => 1
         |    }
         |  }
         |  def liftError(throwable: Throwable): Option[OperationError] = throwable match {
         |    case e: OperationError => Some(e)
         |    case _ => None
         |  }
         |  def unliftError(e: OperationError): Throwable = e
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

  test("smithy4sCodegen.packagePrefix remaps generated package statement") {
    val smithy =
      """
        |$version: "2.0"
        |
        |metadata smithy4sCodegen = { packagePrefix: "internal.generated" }
        |
        |namespace com.example
        |
        |structure Foo {
        |  bar: String
        |}
        |""".stripMargin

    val files = generateScalaCode(smithy)
    val fooFile = files("internal.generated.com.example.Foo")
    assert(fooFile.contains("package internal.generated.com.example"))
  }

  test("smithy4sCodegen.packageMappings remaps with explicit override") {
    val smithy =
      """
        |$version: "2.0"
        |
        |metadata smithy4sCodegen = {
        |  packageMappings: { "com.example": "custom.pkg" }
        |}
        |
        |namespace com.example
        |
        |structure Bar {
        |  x: String
        |}
        |""".stripMargin

    val files = generateScalaCode(smithy)
    val barFile = files("custom.pkg.Bar")
    assert(barFile.contains("package custom.pkg"))
  }

  test(
    "smithy4sCodegen.packageMappings overrides packagePrefix for matched namespace"
  ) {
    val smithy =
      """
        |$version: "2.0"
        |
        |metadata smithy4sCodegen = {
        |  packagePrefix: "prefix",
        |  packageMappings: { "com.example.special": "explicit.pkg" }
        |}
        |
        |namespace com.example.special
        |
        |structure Thing {}
        |""".stripMargin

    val files = generateScalaCode(smithy)
    val thingFile = files("explicit.pkg.Thing")
    assert(thingFile.contains("package explicit.pkg"))
    assert(!thingFile.contains("prefix.com.example.special"))
  }

  test("smithy4sCodegen.packagePrefix applied when no mapping matches") {
    val smithy =
      """
        |$version: "2.0"
        |
        |metadata smithy4sCodegen = {
        |  packagePrefix: "prefix",
        |  packageMappings: { "other.ns": "explicit.pkg" }
        |}
        |
        |namespace com.example
        |
        |structure Baz {}
        |""".stripMargin

    val files = generateScalaCode(smithy)
    val bazFile = files("prefix.com.example.Baz")
    assert(bazFile.contains("package prefix.com.example"))
  }

  test("smithy4sCodegen.excludedNamespaces skips listed namespaces") {
    val smithyConfig =
      """
        |$version: "2.0"
        |
        |metadata smithy4sCodegen = {
        |  excludedNamespaces: ["excluded.ns"]
        |}
        |
        |namespace excluded.ns
        |
        |structure Excluded {}
        |""".stripMargin

    val smithyAllowed =
      """
        |$version: "2.0"
        |
        |namespace allowed.ns
        |
        |structure Allowed {}
        |""".stripMargin

    val files = generateScalaCode(smithyConfig, smithyAllowed)
    assert(files.keys.exists(_.startsWith("allowed.ns")))
    assert(!files.keys.exists(_.startsWith("excluded.ns")))
  }

  test(
    "smithy4sCodegen.allowedNamespaces restricts codegen to listed namespaces"
  ) {
    val smithyConfig =
      """
        |$version: "2.0"
        |
        |metadata smithy4sCodegen = {
        |  allowedNamespaces: ["allowed.ns*"]
        |}
        |
        |namespace allowed.ns
        |
        |structure Allowed {}
        |""".stripMargin

    val smithyNested =
      """
        |$version: "2.0"
        |
        |namespace allowed.ns.nested
        |
        |structure Nested {}
        |""".stripMargin

    val smithyOther =
      """
        |$version: "2.0"
        |
        |namespace other.ns
        |
        |structure Other {}
        |""".stripMargin

    val files = generateScalaCode(smithyConfig, smithyNested, smithyOther)
    assert(files.keys.exists(_.startsWith("allowed.ns.Allowed")))
    assert(files.keys.exists(_.startsWith("allowed.ns.nested.Nested")))
    assert(!files.keys.exists(_.startsWith("other.ns")))
  }

  test(
    "smithy4sCodegen.packagePrefix remaps cross-namespace Type.Ref imports"
  ) {
    val smithyA =
      """
        |$version: "2.0"
        |
        |metadata smithy4sCodegen = { packagePrefix: "gen" }
        |
        |namespace com.a
        |
        |string MyString
        |""".stripMargin

    val smithyB =
      """
        |$version: "2.0"
        |
        |namespace com.b
        |
        |use com.a#MyString
        |
        |structure Outer {
        |  value: MyString
        |}
        |""".stripMargin

    val files = generateScalaCode(smithyA, smithyB)
    // com.a is remapped to gen.com.a; com.b is remapped to gen.com.b
    val outerFile = files("gen.com.b.Outer")
    assert(outerFile.contains("package gen.com.b"))
    // The cross-reference should use the remapped package gen.com.a, not com.a
    assert(outerFile.contains("gen.com.a"))
  }

  test(
    "upstream smithy4sGenerated renderedPackages remaps cross-module Type.Ref"
  ) {
    // Simulates Module A having already generated com.a with packagePrefix "gen".
    // The smithy4sGenerated manifest records the remapping so Module B can resolve it.
    val smithyA =
      """
        |$version: "2.0"
        |
        |metadata smithy4sGenerated = [{
        |  namespaces: ["com.a"],
        |  renderedPackages: { "com.a": "gen.com.a" }
        |}]
        |
        |namespace com.a
        |
        |string MyString
        |""".stripMargin

    val smithyB =
      """
        |$version: "2.0"
        |
        |namespace com.b
        |
        |use com.a#MyString
        |
        |structure Outer {
        |  value: MyString
        |}
        |""".stripMargin

    val files = generateScalaCode(smithyA, smithyB)
    // com.a is skipped — marked as already-generated by the manifest
    assert(
      !files.keys.exists(_.startsWith("com.a")),
      "com.a should be skipped (already generated upstream)"
    )
    val outerFile = files("com.b.Outer")
    assert(outerFile.contains("package com.b"))
    // Cross-module reference must use the remapped package from the upstream manifest
    assert(
      outerFile.contains("gen.com.a"),
      s"Expected gen.com.a in:\n$outerFile"
    )
  }

  private def testErrorsAsUnionsDisabled(smithy: String) = {
    val serviceCode = generateScalaCode(smithy)("smithy4s.errors.ErrorService")

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

    assertContainsSection(serviceCode, "sealed trait OperationError")(
      """|sealed trait OperationError extends scala.Product with scala.Serializable { self =>
         |  @inline final def widen: OperationError = this
         |  def $ordinal: Int
         |  object project {
         |    def badRequest: Option[BadRequest] = OperationError.BadRequestCase.alt.project.lift(self).map(_.badRequest)
         |    def internalServerError: Option[InternalServerError] = OperationError.InternalServerErrorCase.alt.project.lift(self).map(_.internalServerError)
         |  }
         |  def accept[A](visitor: OperationError.Visitor[A]): A = this match {
         |    case value: OperationError.BadRequestCase => visitor.badRequest(value.badRequest)
         |    case value: OperationError.InternalServerErrorCase => visitor.internalServerError(value.internalServerError)
         |  }
         |}""".stripMargin
    )

    assertContainsSection(serviceCode, "object OperationError")(
      """|object OperationError extends ErrorSchema.Companion[OperationError] {
         |  def badRequest(badRequest: BadRequest): OperationError = BadRequestCase(badRequest)
         |  def internalServerError(internalServerError: InternalServerError): OperationError = InternalServerErrorCase(internalServerError)
         |  val id: ShapeId = ShapeId("smithy4s.errors", "OperationError")
         |  val hints: Hints = Hints.empty
         |  final case class BadRequestCase(badRequest: BadRequest) extends OperationError { final def $ordinal: Int = 0 }
         |  final case class InternalServerErrorCase(internalServerError: InternalServerError) extends OperationError { final def $ordinal: Int = 1 }
         |  object BadRequestCase {
         |    val hints: Hints = Hints.empty
         |    val schema: Schema[OperationError.BadRequestCase] = bijection(BadRequest.schema.addHints(hints), OperationError.BadRequestCase(_), _.badRequest)
         |    val alt = schema.oneOf[OperationError]("BadRequest")
         |  }
         |  object InternalServerErrorCase {
         |    val hints: Hints = Hints.empty
         |    val schema: Schema[OperationError.InternalServerErrorCase] = bijection(InternalServerError.schema.addHints(hints), OperationError.InternalServerErrorCase(_), _.internalServerError)
         |    val alt = schema.oneOf[OperationError]("InternalServerError")
         |  }
         |  trait Visitor[A] {
         |    def badRequest(value: BadRequest): A
         |    def internalServerError(value: InternalServerError): A
         |  }
         |  object Visitor {
         |    trait Default[A] extends Visitor[A] {
         |      def default: A
         |      def badRequest(value: BadRequest): A = default
         |      def internalServerError(value: InternalServerError): A = default
         |    }
         |  }
         |  implicit val schema: Schema[OperationError] = union[OperationError](
         |    OperationError.BadRequestCase.alt,
         |    OperationError.InternalServerErrorCase.alt,
         |  ){
         |    _.$ordinal
         |  }
         |  def liftError(throwable: Throwable): Option[OperationError] = throwable match {
         |    case e: BadRequest => Some(OperationError.BadRequestCase(e))
         |    case e: InternalServerError => Some(OperationError.InternalServerErrorCase(e))
         |    case _ => None
         |  }
         |  def unliftError(e: OperationError): Throwable = e match {
         |    case OperationError.BadRequestCase(e) => e
         |    case OperationError.InternalServerErrorCase(e) => e
         |  }
         |}""".stripMargin
    )
  }
}
