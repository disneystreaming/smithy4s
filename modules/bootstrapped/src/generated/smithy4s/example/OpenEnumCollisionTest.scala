package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Prism
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

sealed abstract class OpenEnumCollisionTest(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = OpenEnumCollisionTest
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = OpenEnumCollisionTest
  @inline final def widen: OpenEnumCollisionTest = this
}
object OpenEnumCollisionTest extends Enumeration[OpenEnumCollisionTest] with ShapeTag.Companion[OpenEnumCollisionTest] {
  val id: ShapeId = ShapeId("smithy4s.example", "OpenEnumCollisionTest")

  val hints: Hints = Hints(
    alloy.OpenEnum(),
  ).lazily

  object optics {
    val ONE: Prism[OpenEnumCollisionTest, OpenEnumCollisionTest.ONE.type] = Prism.partial[OpenEnumCollisionTest, OpenEnumCollisionTest.ONE.type]{ case OpenEnumCollisionTest.ONE => OpenEnumCollisionTest.ONE }(identity)
    val TWO: Prism[OpenEnumCollisionTest, OpenEnumCollisionTest.TWO.type] = Prism.partial[OpenEnumCollisionTest, OpenEnumCollisionTest.TWO.type]{ case OpenEnumCollisionTest.TWO => OpenEnumCollisionTest.TWO }(identity)
    val Unknown: Prism[OpenEnumCollisionTest, OpenEnumCollisionTest.Unknown.type] = Prism.partial[OpenEnumCollisionTest, OpenEnumCollisionTest.Unknown.type]{ case OpenEnumCollisionTest.Unknown => OpenEnumCollisionTest.Unknown }(identity)
    val $unknown: Prism[OpenEnumCollisionTest, OpenEnumCollisionTest.$Unknown] = Prism.partial[OpenEnumCollisionTest, OpenEnumCollisionTest.$Unknown]{ case u: OpenEnumCollisionTest.$Unknown => u }(identity)
  }

  case object ONE extends OpenEnumCollisionTest("ONE", "ONE", 0, Hints.empty)
  case object TWO extends OpenEnumCollisionTest("TWO", "TWO", 1, Hints.empty)
  case object Unknown extends OpenEnumCollisionTest("Unknown", "Unknown", 2, Hints.empty)
  final case class $Unknown(str: String) extends OpenEnumCollisionTest(str, "$Unknown", -1, Hints.empty)

  val $unknown: String => OpenEnumCollisionTest = $Unknown(_)

  val values: List[OpenEnumCollisionTest] = List(
    ONE,
    TWO,
    Unknown,
  )
  val tag: EnumTag[OpenEnumCollisionTest] = EnumTag.OpenStringEnum($unknown)
  implicit val schema: Schema[OpenEnumCollisionTest] = enumeration(tag, values).withId(id).addHints(hints)
}
