/*
 *  Copyright 2021-2025 Disney Streaming
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

final class DynamicHintRenderingSpec extends munit.FunSuite {

  test("dynamic hint rendering mode - structure") {
    val smithySpec = """|$version: "2.0"
                        |
                        |metadata smithy4sRenderDynamicHintBindings = true
                        |
                        |namespace test
                        |
                        |@input
                        |structure Test {
                        | @required
                        | @jsonName("field_one")
                        | @httpHeader("x-one")
                        | one: String
                        |
                        | @timestampFormat("date-time")
                        | two: Timestamp
                        |}
                        |
                        |""".stripMargin

    val scalaCode =
      """|package test
         |
         |import smithy4s.Hints
         |import smithy4s.Schema
         |import smithy4s.ShapeId
         |import smithy4s.ShapeTag
         |import smithy4s.Timestamp
         |import smithy4s.schema.Schema.string
         |import smithy4s.schema.Schema.struct
         |import smithy4s.schema.Schema.timestamp
         |
         |final case class Test(one: String, two: Option[Timestamp] = None)
         |
         |object Test extends ShapeTag.Companion[Test] {
         |  val id: ShapeId = ShapeId("test", "Test")
         |
         |  val hints: Hints = Hints(
         |    Hints.dynamic(ShapeId("smithy.api", "input"), smithy4s.Document.obj()),
         |  )
         |
         |  // constructor using the original order from the spec
         |  private def make(one: String, two: Option[Timestamp]): Test = Test(one, two)
         |
         |  implicit val schema: Schema[Test] = struct(
         |    string.required[Test]("one", _.one).addHints(Hints.dynamic(ShapeId("smithy.api", "httpHeader"), smithy4s.Document.fromString("x-one")), Hints.dynamic(ShapeId("smithy.api", "jsonName"), smithy4s.Document.fromString("field_one"))),
         |    timestamp.optional[Test]("two", _.two).addHints(Hints.dynamic(ShapeId("smithy.api", "timestampFormat"), smithy4s.Document.fromString("date-time"))),
         |  )(make).withId(id).addHints(hints)
         |}
         |""".stripMargin

    TestUtils.runTest(smithySpec, scalaCode)
  }

  test("dynamic hint rendering mode - enum") {
    val smithySpec = """|$version: "2.0"
                        |
                        |metadata smithy4sRenderDynamicHintBindings = true
                        |
                        |namespace test
                        |
                        |@documentation("hello")
                        |enum Test {
                        | @since("1")
                        | ONE = "one"
                        |}
                        |
                        |""".stripMargin

    val scalaCode =
      """|package test
         |
         |import smithy4s.Enumeration
         |import smithy4s.Hints
         |import smithy4s.Schema
         |import smithy4s.ShapeId
         |import smithy4s.ShapeTag
         |import smithy4s.schema.EnumTag
         |import smithy4s.schema.Schema.enumeration
         |
         |/** hello */
         |sealed abstract class Test(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
         |  override type EnumType = Test
         |  override val value: String = _value
         |  override val name: String = _name
         |  override val intValue: Int = _intValue
         |  override val hints: Hints = _hints
         |  override def enumeration: Enumeration[EnumType] = Test
         |  @inline final def widen: Test = this
         |}
         |object Test extends Enumeration[Test] with ShapeTag.Companion[Test] {
         |  val id: ShapeId = ShapeId("test", "Test")
         |
         |  val hints: Hints = Hints(
         |    Hints.dynamic(ShapeId("smithy.api", "documentation"), smithy4s.Document.fromString("hello")),
         |  )
         |
         |  case object ONE extends Test("one", "ONE", 0, Hints.empty) {
         |    override val hints: Hints = Hints(Hints.dynamic(ShapeId("smithy.api", "since"), smithy4s.Document.fromString("1")))
         |  }
         |
         |  val values: List[Test] = List(
         |    ONE,
         |  )
         |  val tag: EnumTag[Test] = EnumTag.ClosedStringEnum
         |  implicit val schema: Schema[Test] = enumeration(tag, values).withId(id).addHints(hints)
         |}
         |""".stripMargin

    TestUtils.runTest(smithySpec, scalaCode)
  }

  test("dynamic hint rendering mode - union") {
    val smithySpec = """|$version: "2.0"
                        |
                        |metadata smithy4sRenderDynamicHintBindings = true
                        |
                        |namespace test
                        |
                        |@documentation("hello")
                        |union Test {
                        | @since("1")
                        | one: String
                        | two: Integer
                        |}
                        |
                        |""".stripMargin

    val scalaCode =
      """|package test
         |
         |import smithy4s.Hints
         |import smithy4s.Schema
         |import smithy4s.ShapeId
         |import smithy4s.ShapeTag
         |import smithy4s.schema.Schema.bijection
         |import smithy4s.schema.Schema.int
         |import smithy4s.schema.Schema.string
         |import smithy4s.schema.Schema.union
         |
         |/** hello */
         |sealed trait Test extends scala.Product with scala.Serializable { self =>
         |  @inline final def widen: Test = this
         |  def $ordinal: Int
         |
         |  object project {
         |    def one: Option[String] = Test.OneCase.alt.project.lift(self).map(_.one)
         |    def two: Option[Int] = Test.TwoCase.alt.project.lift(self).map(_.two)
         |  }
         |
         |  def accept[A](visitor: Test.Visitor[A]): A = this match {
         |    case value: Test.OneCase => visitor.one(value.one)
         |    case value: Test.TwoCase => visitor.two(value.two)
         |  }
         |}
         |object Test extends ShapeTag.Companion[Test] {
         |
         |  def one(one: String): Test = OneCase(one)
         |  def two(two: Int): Test = TwoCase(two)
         |
         |  val id: ShapeId = ShapeId("test", "Test")
         |
         |  val hints: Hints = Hints(
         |    Hints.dynamic(ShapeId("smithy.api", "documentation"), smithy4s.Document.fromString("hello")),
         |  )
         |
         |  final case class OneCase(one: String) extends Test { final def $ordinal: Int = 0 }
         |  final case class TwoCase(two: Int) extends Test { final def $ordinal: Int = 1 }
         |
         |  object OneCase {
         |    val hints: Hints = Hints(
         |      Hints.dynamic(ShapeId("smithy.api", "since"), smithy4s.Document.fromString("1")),
         |    )
         |    val schema: Schema[Test.OneCase] = bijection(string.addHints(hints), Test.OneCase(_), _.one)
         |    val alt = schema.oneOf[Test]("one")
         |  }
         |  object TwoCase {
         |    val hints: Hints = Hints.empty
         |    val schema: Schema[Test.TwoCase] = bijection(int.addHints(hints), Test.TwoCase(_), _.two)
         |    val alt = schema.oneOf[Test]("two")
         |  }
         |
         |  trait Visitor[A] {
         |    def one(value: String): A
         |    def two(value: Int): A
         |  }
         |
         |  object Visitor {
         |    trait Default[A] extends Visitor[A] {
         |      def default: A
         |      def one(value: String): A = default
         |      def two(value: Int): A = default
         |    }
         |  }
         |
         |  implicit val schema: Schema[Test] = union(
         |    Test.OneCase.alt,
         |    Test.TwoCase.alt,
         |  ){
         |    _.$ordinal
         |  }.withId(id).addHints(hints)
         |}
         |""".stripMargin

    TestUtils.runTest(smithySpec, scalaCode)
  }

  test("dynamic hint rendering mode - primitive") {
    val smithySpec = """|$version: "2.0"
                        |
                        |metadata smithy4sRenderDynamicHintBindings = true
                        |
                        |namespace test
                        |
                        |@since("1")
                        |string Test
                        |""".stripMargin

    val scalaCode1 =
      """|package test
         |
         |import smithy4s.Hints
         |import smithy4s.Newtype
         |import smithy4s.Schema
         |import smithy4s.ShapeId
         |import smithy4s.schema.Schema.bijection
         |import smithy4s.schema.Schema.string
         |
         |object Test extends Newtype[String] {
         |  val id: ShapeId = ShapeId("test", "Test")
         |  val hints: Hints = Hints(
         |    Hints.dynamic(ShapeId("smithy.api", "since"), smithy4s.Document.fromString("1")),
         |  )
         |  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints)
         |  implicit val schema: Schema[Test] = bijection(underlyingSchema, asBijection)
         |}
         |""".stripMargin

    val scalaCode2 =
      """|package object test {
         |
         |  type Test = test.Test.Type
         |
         |}
         |""".stripMargin

    TestUtils.runTest(smithySpec, scalaCode1, scalaCode2)
  }

  test("dynamic hint rendering mode - service") {
    val smithySpec = """|$version: "2.0"
                        |
                        |metadata smithy4sRenderDynamicHintBindings = true
                        |
                        |namespace test
                        |
                        |@since("1")
                        |service Test {
                        |  operations: [One]
                        |}
                        |
                        |@since("2")
                        |operation One {}
                        |
                        |""".stripMargin

    val scalaCode1 =
      """|package object test {
         |  type Test[F[_]] = smithy4s.kinds.FunctorAlgebra[TestGen, F]
         |  val Test = TestGen
         |
         |
         |}
         |""".stripMargin

    val scalaCode2 =
      """|package test
         |
         |import smithy4s.Endpoint
         |import smithy4s.Hints
         |import smithy4s.Schema
         |import smithy4s.Service
         |import smithy4s.ShapeId
         |import smithy4s.Transformation
         |import smithy4s.kinds.PolyFunction5
         |import smithy4s.kinds.toPolyFunction5.const5
         |import smithy4s.schema.OperationSchema
         |import smithy4s.schema.Schema.unit
         |
         |trait TestGen[F[_, _, _, _, _]] {
         |  self =>
         |
         |  def one(): F[Unit, Nothing, Unit, Nothing, Nothing]
         |
         |  final def transform: Transformation.PartiallyApplied[TestGen[F]] = Transformation.of[TestGen[F]](this)
         |}
         |
         |object TestGen extends Service.Mixin[TestGen, TestOperation] {
         |
         |  val id: ShapeId = ShapeId("test", "Test")
         |  val version: String = ""
         |
         |  val hints: Hints = Hints(
         |    Hints.dynamic(ShapeId("smithy.api", "since"), smithy4s.Document.fromString("1")),
         |  )
         |
         |  def apply[F[_]](implicit F: Impl[F]): F.type = F
         |
         |  object ErrorAware {
         |    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
         |    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
         |  }
         |
         |  val endpoints: Vector[smithy4s.Endpoint[TestOperation, _, _, _, _, _]] = Vector(
         |    TestOperation.One,
         |  )
         |
         |  def input[I, E, O, SI, SO](op: TestOperation[I, E, O, SI, SO]): I = op.input
         |  def ordinal[I, E, O, SI, SO](op: TestOperation[I, E, O, SI, SO]): Int = op.ordinal
         |  override def endpoint[I, E, O, SI, SO](op: TestOperation[I, E, O, SI, SO]) = op.endpoint
         |  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends TestOperation.Transformed[TestOperation, P](reified, const5(value))
         |  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
         |  def reified: TestGen[TestOperation] = TestOperation.reified
         |  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: TestGen[P], f: PolyFunction5[P, P1]): TestGen[P1] = new TestOperation.Transformed(alg, f)
         |  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[TestOperation, P]): TestGen[P] = new TestOperation.Transformed(reified, f)
         |  def toPolyFunction[P[_, _, _, _, _]](impl: TestGen[P]): PolyFunction5[TestOperation, P] = TestOperation.toPolyFunction(impl)
         |
         |}
         |
         |sealed trait TestOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
         |  def run[F[_, _, _, _, _]](impl: TestGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
         |  def ordinal: Int
         |  def input: Input
         |  def endpoint: Endpoint[TestOperation, Input, Err, Output, StreamedInput, StreamedOutput]
         |}
         |
         |object TestOperation {
         |
         |  object reified extends TestGen[TestOperation] {
         |    def one(): One = One()
         |  }
         |  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: TestGen[P], f: PolyFunction5[P, P1]) extends TestGen[P1] {
         |    def one(): P1[Unit, Nothing, Unit, Nothing, Nothing] = f[Unit, Nothing, Unit, Nothing, Nothing](alg.one())
         |  }
         |
         |  def toPolyFunction[P[_, _, _, _, _]](impl: TestGen[P]): PolyFunction5[TestOperation, P] = new PolyFunction5[TestOperation, P] {
         |    def apply[I, E, O, SI, SO](op: TestOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
         |  }
         |  final case class One() extends TestOperation[Unit, Nothing, Unit, Nothing, Nothing] {
         |    def run[F[_, _, _, _, _]](impl: TestGen[F]): F[Unit, Nothing, Unit, Nothing, Nothing] = impl.one()
         |    def ordinal: Int = 0
         |    def input: Unit = ()
         |    def endpoint: smithy4s.Endpoint[TestOperation,Unit, Nothing, Unit, Nothing, Nothing] = One
         |  }
         |  object One extends smithy4s.Endpoint[TestOperation,Unit, Nothing, Unit, Nothing, Nothing] {
         |    val schema: OperationSchema[Unit, Nothing, Unit, Nothing, Nothing] = Schema.operation(ShapeId("test", "One"))
         |      .withInput(unit)
         |      .withOutput(unit)
         |      .withHints(Hints.dynamic(ShapeId("smithy.api", "since"), smithy4s.Document.fromString("2")))
         |    def wrap(input: Unit): One = One()
         |  }
         |}
         |""".stripMargin

    TestUtils.runTest(smithySpec, scalaCode1, scalaCode2)
  }

}
