package smithy4s.example

import smithy4s.schema.Schema._

sealed trait TestMixinAdt extends scala.Product with scala.Serializable {
  @inline final def widen: TestMixinAdt = this
}
object TestMixinAdt extends smithy4s.ShapeTag.Companion[TestMixinAdt] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "TestMixinAdt")

  val hints : smithy4s.Hints = smithy4s.Hints.empty

  case class TestAdtMemberWithMixin(a: Option[String] = None, b: Option[Int] = None) extends TestMixinAdt with CommonFieldsOne
  object TestAdtMemberWithMixin extends smithy4s.ShapeTag.Companion[TestAdtMemberWithMixin] {
    val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "TestAdtMemberWithMixin")

    val hints : smithy4s.Hints = smithy4s.Hints.empty

    val schema: smithy4s.Schema[TestAdtMemberWithMixin] = struct(
      string.optional[TestAdtMemberWithMixin]("a", _.a),
      int.optional[TestAdtMemberWithMixin]("b", _.b),
    ){
      TestAdtMemberWithMixin.apply
    }.withId(id).addHints(hints)

    val alt = schema.oneOf[TestMixinAdt]("test")
  }


  implicit val schema: smithy4s.Schema[TestMixinAdt] = union(
    TestAdtMemberWithMixin.alt,
  ){
    case c : TestAdtMemberWithMixin => TestAdtMemberWithMixin.alt(c)
  }.withId(id).addHints(hints)
}