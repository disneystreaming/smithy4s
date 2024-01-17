package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Prism
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

sealed abstract class OpenEnumCollisionTest2(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = OpenEnumCollisionTest2
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = OpenEnumCollisionTest2
  @inline final def widen: OpenEnumCollisionTest2 = this
}
object OpenEnumCollisionTest2 extends Enumeration[OpenEnumCollisionTest2] with ShapeTag.Companion[OpenEnumCollisionTest2] {
  val id: ShapeId = ShapeId("smithy4s.example", "OpenEnumCollisionTest2")

  val hints: Hints = Hints(
    alloy.OpenEnum(),
  ).lazily

  object optics {
    val ONE: Prism[OpenEnumCollisionTest2, OpenEnumCollisionTest2.ONE.type] = Prism.partial[OpenEnumCollisionTest2, OpenEnumCollisionTest2.ONE.type]{ case OpenEnumCollisionTest2.ONE => OpenEnumCollisionTest2.ONE }(identity)
    val TWO: Prism[OpenEnumCollisionTest2, OpenEnumCollisionTest2.TWO.type] = Prism.partial[OpenEnumCollisionTest2, OpenEnumCollisionTest2.TWO.type]{ case OpenEnumCollisionTest2.TWO => OpenEnumCollisionTest2.TWO }(identity)
    val THREE: Prism[OpenEnumCollisionTest2, OpenEnumCollisionTest2.THREE.type] = Prism.partial[OpenEnumCollisionTest2, OpenEnumCollisionTest2.THREE.type]{ case OpenEnumCollisionTest2.THREE => OpenEnumCollisionTest2.THREE }(identity)
    val $unknown: Prism[OpenEnumCollisionTest2, OpenEnumCollisionTest2.$Unknown] = Prism.partial[OpenEnumCollisionTest2, OpenEnumCollisionTest2.$Unknown]{ case u: OpenEnumCollisionTest2.$Unknown => u }(identity)
  }

  case object ONE extends OpenEnumCollisionTest2("ONE", "ONE", 0, Hints.empty)
  case object TWO extends OpenEnumCollisionTest2("TWO", "TWO", 1, Hints.empty)
  case object THREE extends OpenEnumCollisionTest2("unknown", "THREE", 2, Hints.empty)
  final case class $Unknown(str: String) extends OpenEnumCollisionTest2(str, "$Unknown", -1, Hints.empty)

  val $unknown: String => OpenEnumCollisionTest2 = $Unknown(_)

  val values: List[OpenEnumCollisionTest2] = List(
    ONE,
    TWO,
    THREE,
  )
  val tag: EnumTag[OpenEnumCollisionTest2] = EnumTag.OpenStringEnum($unknown)
  implicit val schema: Schema[OpenEnumCollisionTest2] = enumeration(tag, values).withId(id).addHints(hints)
}
