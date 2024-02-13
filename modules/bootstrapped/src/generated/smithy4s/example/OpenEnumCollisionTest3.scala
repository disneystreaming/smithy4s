package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Prism
import smithy4s.schema.Schema.openStringEnumeration

sealed abstract class OpenEnumCollisionTest3(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = OpenEnumCollisionTest3
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = OpenEnumCollisionTest3
  @inline final def widen: OpenEnumCollisionTest3 = this
}
object OpenEnumCollisionTest3 extends Enumeration[OpenEnumCollisionTest3] with ShapeTag.Companion[OpenEnumCollisionTest3] {
  val id: ShapeId = ShapeId("smithy4s.example", "OpenEnumCollisionTest3")

  val hints: Hints = Hints(
    alloy.OpenEnum(),
  ).lazily

  object optics {
    val ONE: Prism[OpenEnumCollisionTest3, OpenEnumCollisionTest3.ONE.type] = Prism.partial[OpenEnumCollisionTest3, OpenEnumCollisionTest3.ONE.type]{ case OpenEnumCollisionTest3.ONE => OpenEnumCollisionTest3.ONE }(identity)
    val TWO: Prism[OpenEnumCollisionTest3, OpenEnumCollisionTest3.TWO.type] = Prism.partial[OpenEnumCollisionTest3, OpenEnumCollisionTest3.TWO.type]{ case OpenEnumCollisionTest3.TWO => OpenEnumCollisionTest3.TWO }(identity)
    val unknown: Prism[OpenEnumCollisionTest3, OpenEnumCollisionTest3.unknown.type] = Prism.partial[OpenEnumCollisionTest3, OpenEnumCollisionTest3.unknown.type]{ case OpenEnumCollisionTest3.unknown => OpenEnumCollisionTest3.unknown }(identity)
    val $unknown: Prism[OpenEnumCollisionTest3, OpenEnumCollisionTest3.$Unknown] = Prism.partial[OpenEnumCollisionTest3, OpenEnumCollisionTest3.$Unknown]{ case u: OpenEnumCollisionTest3.$Unknown => u }(identity)
  }

  case object ONE extends OpenEnumCollisionTest3("ONE", "ONE", 0, Hints.empty)
  case object TWO extends OpenEnumCollisionTest3("TWO", "TWO", 1, Hints.empty)
  case object unknown extends OpenEnumCollisionTest3("unknown", "unknown", 2, Hints.empty)
  final case class $Unknown(str: String) extends OpenEnumCollisionTest3("$Unknown", str, -1, Hints.empty)

  val $unknown: String => OpenEnumCollisionTest3 = $Unknown(_)

  val values: List[OpenEnumCollisionTest3] = List(
    ONE,
    TWO,
    unknown,
  )
  implicit val schema: Schema[OpenEnumCollisionTest3] = openStringEnumeration(values, $unknown).withId(id).addHints(hints)
}
