package smithy4s.example

import _root_.smithy4s.Enumeration
import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.EnumTag
import _root_.smithy4s.schema.Schema.enumeration
import smithy4s.optics.Prism

sealed abstract class OpenIntEnumTest(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = OpenIntEnumTest
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = OpenIntEnumTest
  @inline final def widen: OpenIntEnumTest = this
}
object OpenIntEnumTest extends Enumeration[OpenIntEnumTest] with ShapeTag.Companion[OpenIntEnumTest] {
  val id: ShapeId = ShapeId("smithy4s.example", "OpenIntEnumTest")

  val hints: Hints = Hints(
    alloy.OpenEnum(),
  )

  object optics {
    val ONE: Prism[OpenIntEnumTest, OpenIntEnumTest.ONE.type] = Prism.partial[OpenIntEnumTest, OpenIntEnumTest.ONE.type]{ case OpenIntEnumTest.ONE => OpenIntEnumTest.ONE }(identity)
    val $unknown: Prism[OpenIntEnumTest, OpenIntEnumTest.$Unknown] = Prism.partial[OpenIntEnumTest, OpenIntEnumTest.$Unknown]{ case u: OpenIntEnumTest.$Unknown => u }(identity)
  }

  case object ONE extends OpenIntEnumTest("ONE", "ONE", 1, Hints())
  final case class $Unknown(int: Int) extends OpenIntEnumTest("$Unknown", "$Unknown", int, Hints.empty)

  val $unknown: Int => OpenIntEnumTest = $Unknown(_)

  val values: List[OpenIntEnumTest] = List(
    ONE,
  )
  val tag: EnumTag[OpenIntEnumTest] = EnumTag.OpenIntEnum($unknown)
  implicit val schema: Schema[OpenIntEnumTest] = enumeration(tag, values).withId(id).addHints(hints)
}
