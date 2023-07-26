package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.union

sealed trait TestMixinAdt extends scala.Product with scala.Serializable {
  @inline final def widen: TestMixinAdt = this
  def _ordinal: Int
}
object TestMixinAdt extends ShapeTag.$Companion[TestMixinAdt] {

  def testAdtMemberWithMixin(a: Option[String] = None, b: Option[Int] = None): TestMixinAdt = TestAdtMemberWithMixin(a, b)

  val $id: ShapeId = ShapeId("smithy4s.example", "TestMixinAdt")

  val $hints: Hints = Hints.empty

  final case class TestAdtMemberWithMixin(a: Option[String] = None, b: Option[Int] = None) extends TestMixinAdt with CommonFieldsOne {
    def _ordinal: Int = 0
  }
  object TestAdtMemberWithMixin extends ShapeTag.$Companion[TestAdtMemberWithMixin] {
    val $id: ShapeId = ShapeId("smithy4s.example", "TestAdtMemberWithMixin")

    val $hints: Hints = Hints.empty

    val a: FieldLens[TestAdtMemberWithMixin, Option[String]] = string.optional[TestAdtMemberWithMixin]("a", _.a, n => c => c.copy(a = n))
    val b: FieldLens[TestAdtMemberWithMixin, Option[Int]] = int.optional[TestAdtMemberWithMixin]("b", _.b, n => c => c.copy(b = n))

    val $schema: Schema[TestAdtMemberWithMixin] = struct(
      a,
      b,
    ){
      TestAdtMemberWithMixin.apply
    }.withId($id).addHints($hints)
  }


  val test = TestAdtMemberWithMixin.$schema.oneOf[TestMixinAdt]("test")

  implicit val $schema: Schema[TestMixinAdt] = union(
    test,
  ){
    _._ordinal
  }.withId($id).addHints($hints)
}