package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.union

sealed trait TestMixinAdt extends scala.Product with scala.Serializable {
  @inline final def widen: TestMixinAdt = this
  def $ordinal: Int
}
object TestMixinAdt extends ShapeTag.Companion[TestMixinAdt] {

  def testAdtMemberWithMixin(a: Option[String] = None, b: Option[Int] = None):TestAdtMemberWithMixin = TestAdtMemberWithMixin(a, b)

  val id: ShapeId = ShapeId("smithy4s.example", "TestMixinAdt")

  val hints: Hints = Hints.empty

  final case class TestAdtMemberWithMixin(a: Option[String] = None, b: Option[Int] = None) extends TestMixinAdt with CommonFieldsOne {
    def $ordinal: Int = 0
  }
  object TestAdtMemberWithMixin extends ShapeTag.Companion[TestAdtMemberWithMixin] {
    val id: ShapeId = ShapeId("smithy4s.example", "TestAdtMemberWithMixin")

    val hints: Hints = Hints.empty

    val schema: Schema[TestAdtMemberWithMixin] = struct(
      string.optional[TestAdtMemberWithMixin]("a", _.a),
      int.optional[TestAdtMemberWithMixin]("b", _.b),
    ){
      TestAdtMemberWithMixin.apply
    }.withId(id).addHints(hints)

    val alt = schema.oneOf[TestMixinAdt]("test")
  }


  implicit val schema: Schema[TestMixinAdt] = union(
    TestAdtMemberWithMixin.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
