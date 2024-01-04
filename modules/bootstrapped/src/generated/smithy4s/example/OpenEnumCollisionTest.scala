package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Prism
import smithy4s.schema.Schema.openStringEnumeration

sealed abstract class OpenEnumCollisionTest(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = OpenEnumCollisionTest
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = OpenEnumCollisionTest
  @inline final def widen: OpenEnumCollisionTest = this
}
object OpenEnumCollisionTest extends Enumeration[OpenEnumCollisionTest] with ShapeTag.Companion[OpenEnumCollisionTest] {
  val id: ShapeId = ShapeId("smithy4s.example", "OpenEnumCollisionTest")

  val hints: Hints = Hints(
    alloy.OpenEnum(),
  )

  object optics {
    val ONE: Prism[OpenEnumCollisionTest, OpenEnumCollisionTest.ONE.type] = Prism.partial[OpenEnumCollisionTest, OpenEnumCollisionTest.ONE.type]{ case OpenEnumCollisionTest.ONE => OpenEnumCollisionTest.ONE }(identity)
    val TWO: Prism[OpenEnumCollisionTest, OpenEnumCollisionTest.TWO.type] = Prism.partial[OpenEnumCollisionTest, OpenEnumCollisionTest.TWO.type]{ case OpenEnumCollisionTest.TWO => OpenEnumCollisionTest.TWO }(identity)
    val Unknown: Prism[OpenEnumCollisionTest, OpenEnumCollisionTest.Unknown.type] = Prism.partial[OpenEnumCollisionTest, OpenEnumCollisionTest.Unknown.type]{ case OpenEnumCollisionTest.Unknown => OpenEnumCollisionTest.Unknown }(identity)
    val $unknown: Prism[OpenEnumCollisionTest, OpenEnumCollisionTest.$Unknown] = Prism.partial[OpenEnumCollisionTest, OpenEnumCollisionTest.$Unknown]{ case u: OpenEnumCollisionTest.$Unknown => u }(identity)
  }

  case object ONE extends OpenEnumCollisionTest("ONE", "ONE", 0, Hints())
  case object TWO extends OpenEnumCollisionTest("TWO", "TWO", 1, Hints())
  case object Unknown extends OpenEnumCollisionTest("Unknown", "Unknown", 2, Hints())
  final case class $Unknown(str: String) extends OpenEnumCollisionTest("$Unknown", str, -1, Hints.empty)

  val $unknown: String => OpenEnumCollisionTest = $Unknown(_)

  val values: List[OpenEnumCollisionTest] = List(
    ONE,
    TWO,
    Unknown,
  )
  implicit val schema: Schema[OpenEnumCollisionTest] = openStringEnumeration(values, $unknown).withId(id).addHints(hints)
}
