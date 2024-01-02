package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Prism
import smithy4s.schema.Schema.openIntEnumeration

sealed abstract class OpenIntEnumTest(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = OpenIntEnumTest
  override val name: String = _name
  override val stringValue: String = _stringValue
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
  implicit val schema: Schema[OpenIntEnumTest] = openIntEnumeration(values, $unknown).withId(id).addHints(hints)
}
