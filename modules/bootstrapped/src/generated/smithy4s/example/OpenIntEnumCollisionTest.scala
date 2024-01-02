package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Prism
import smithy4s.schema.Schema.openIntEnumeration

sealed abstract class OpenIntEnumCollisionTest(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = OpenIntEnumCollisionTest
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = OpenIntEnumCollisionTest
  @inline final def widen: OpenIntEnumCollisionTest = this
}
object OpenIntEnumCollisionTest extends Enumeration[OpenIntEnumCollisionTest] with ShapeTag.Companion[OpenIntEnumCollisionTest] {
  val id: ShapeId = ShapeId("smithy4s.example", "OpenIntEnumCollisionTest")

  val hints: Hints = Hints(
    alloy.OpenEnum(),
  )

  object optics {
    val ONE: Prism[OpenIntEnumCollisionTest, OpenIntEnumCollisionTest.ONE.type] = Prism.partial[OpenIntEnumCollisionTest, OpenIntEnumCollisionTest.ONE.type]{ case OpenIntEnumCollisionTest.ONE => OpenIntEnumCollisionTest.ONE }(identity)
    val TWO: Prism[OpenIntEnumCollisionTest, OpenIntEnumCollisionTest.TWO.type] = Prism.partial[OpenIntEnumCollisionTest, OpenIntEnumCollisionTest.TWO.type]{ case OpenIntEnumCollisionTest.TWO => OpenIntEnumCollisionTest.TWO }(identity)
    val Unknown: Prism[OpenIntEnumCollisionTest, OpenIntEnumCollisionTest.Unknown.type] = Prism.partial[OpenIntEnumCollisionTest, OpenIntEnumCollisionTest.Unknown.type]{ case OpenIntEnumCollisionTest.Unknown => OpenIntEnumCollisionTest.Unknown }(identity)
    val $unknown: Prism[OpenIntEnumCollisionTest, OpenIntEnumCollisionTest.$Unknown] = Prism.partial[OpenIntEnumCollisionTest, OpenIntEnumCollisionTest.$Unknown]{ case u: OpenIntEnumCollisionTest.$Unknown => u }(identity)
  }

  case object ONE extends OpenIntEnumCollisionTest("ONE", "ONE", 1, Hints())
  case object TWO extends OpenIntEnumCollisionTest("TWO", "TWO", 2, Hints())
  case object Unknown extends OpenIntEnumCollisionTest("Unknown", "Unknown", 3, Hints())
  final case class $Unknown(int: Int) extends OpenIntEnumCollisionTest("$Unknown", "$Unknown", int, Hints.empty)

  val $unknown: Int => OpenIntEnumCollisionTest = $Unknown(_)

  val values: List[OpenIntEnumCollisionTest] = List(
    ONE,
    TWO,
    Unknown,
  )
  implicit val schema: Schema[OpenIntEnumCollisionTest] = openIntEnumeration(values, $unknown).withId(id).addHints(hints)
}
