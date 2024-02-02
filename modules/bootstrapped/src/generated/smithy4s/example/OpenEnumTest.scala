package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Prism
import smithy4s.schema.Schema.openStringEnumeration

sealed abstract class OpenEnumTest(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = OpenEnumTest
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = OpenEnumTest
  @inline final def widen: OpenEnumTest = this
}
object OpenEnumTest extends Enumeration[OpenEnumTest] with ShapeTag.Companion[OpenEnumTest] {
  val id: ShapeId = ShapeId("smithy4s.example", "OpenEnumTest")

  val hints: Hints = Hints(
    alloy.OpenEnum(),
  ).lazily

  object optics {
    val ONE: Prism[OpenEnumTest, OpenEnumTest.ONE.type] = Prism.partial[OpenEnumTest, OpenEnumTest.ONE.type]{ case OpenEnumTest.ONE => OpenEnumTest.ONE }(identity)
    val $unknown: Prism[OpenEnumTest, OpenEnumTest.$Unknown] = Prism.partial[OpenEnumTest, OpenEnumTest.$Unknown]{ case u: OpenEnumTest.$Unknown => u }(identity)
  }

  case object ONE extends OpenEnumTest("ONE", "ONE", 0, Hints.empty)
  final case class $Unknown(str: String) extends OpenEnumTest("$Unknown", str, -1, Hints.empty)

  val $unknown: String => OpenEnumTest = $Unknown(_)

  val values: List[OpenEnumTest] = List(
    ONE,
  )
  implicit val schema: Schema[OpenEnumTest] = openStringEnumeration(values, $unknown).withId(id).addHints(hints)
}
