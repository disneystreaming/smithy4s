package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Prism
import smithy4s.schema.Schema.openIntEnumeration

sealed abstract class OpenIntEnumCollisionTest2(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = OpenIntEnumCollisionTest2
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = OpenIntEnumCollisionTest2
  @inline final def widen: OpenIntEnumCollisionTest2 = this
}
object OpenIntEnumCollisionTest2 extends Enumeration[OpenIntEnumCollisionTest2] with ShapeTag.Companion[OpenIntEnumCollisionTest2] {
  val id: ShapeId = ShapeId("smithy4s.example", "OpenIntEnumCollisionTest2")

  val hints: Hints = Hints(
    alloy.OpenEnum(),
  ).lazily

  object optics {
    val ONE: Prism[OpenIntEnumCollisionTest2, OpenIntEnumCollisionTest2.ONE.type] = Prism.partial[OpenIntEnumCollisionTest2, OpenIntEnumCollisionTest2.ONE.type]{ case OpenIntEnumCollisionTest2.ONE => OpenIntEnumCollisionTest2.ONE }(identity)
    val TWO: Prism[OpenIntEnumCollisionTest2, OpenIntEnumCollisionTest2.TWO.type] = Prism.partial[OpenIntEnumCollisionTest2, OpenIntEnumCollisionTest2.TWO.type]{ case OpenIntEnumCollisionTest2.TWO => OpenIntEnumCollisionTest2.TWO }(identity)
    val unknown: Prism[OpenIntEnumCollisionTest2, OpenIntEnumCollisionTest2.unknown.type] = Prism.partial[OpenIntEnumCollisionTest2, OpenIntEnumCollisionTest2.unknown.type]{ case OpenIntEnumCollisionTest2.unknown => OpenIntEnumCollisionTest2.unknown }(identity)
    val $unknown: Prism[OpenIntEnumCollisionTest2, OpenIntEnumCollisionTest2.$Unknown] = Prism.partial[OpenIntEnumCollisionTest2, OpenIntEnumCollisionTest2.$Unknown]{ case u: OpenIntEnumCollisionTest2.$Unknown => u }(identity)
  }

  case object ONE extends OpenIntEnumCollisionTest2("ONE", "ONE", 1, Hints.empty)
  case object TWO extends OpenIntEnumCollisionTest2("TWO", "TWO", 2, Hints.empty)
  case object unknown extends OpenIntEnumCollisionTest2("unknown", "unknown", 3, Hints.empty)
  final case class $Unknown(int: Int) extends OpenIntEnumCollisionTest2("$Unknown", "$Unknown", int, Hints.empty)

  val $unknown: Int => OpenIntEnumCollisionTest2 = $Unknown(_)

  val values: List[OpenIntEnumCollisionTest2] = List(
    ONE,
    TWO,
    unknown,
  )
  implicit val schema: Schema[OpenIntEnumCollisionTest2] = openIntEnumeration(values, $unknown).withId(id).addHints(hints)
}
