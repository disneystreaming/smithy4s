package smithy4s.example.bincompat

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

sealed abstract class BincompatOpenEnum(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = BincompatOpenEnum
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = BincompatOpenEnum
  @inline final def widen: BincompatOpenEnum = this
}
object BincompatOpenEnum extends Enumeration[BincompatOpenEnum] with ShapeTag.Companion[BincompatOpenEnum] {
  val id: ShapeId = ShapeId("smithy4s.example.bincompat", "BincompatOpenEnum")

  val hints: Hints = Hints(
    alloy.OpenEnum(),
  ).lazily

  private object impl {
    case object A extends BincompatOpenEnum("A", "A", 0, Hints.empty)
    case object B extends BincompatOpenEnum("B", "B", 1, Hints.empty)
    case object C extends BincompatOpenEnum("C", "C", 2, Hints.empty)
  }

  val A: BincompatOpenEnum = impl.A
  val B: BincompatOpenEnum = impl.B
  val C: BincompatOpenEnum = impl.C
  final case class $Unknown(str: String) extends BincompatOpenEnum(str, "$Unknown", -1, Hints.empty)

  val $unknown: String => BincompatOpenEnum = $Unknown(_)

  def fromStringOrUnknown(s: String): BincompatOpenEnum = fromString(s).getOrElse($unknown(s))

  val values: List[BincompatOpenEnum] = List(
    A,
    B,
    C,
  )
  val tag: EnumTag[BincompatOpenEnum] = EnumTag.OpenStringEnum($unknown)
  implicit val schema: Schema[BincompatOpenEnum] = enumeration(tag, values).withId(id).addHints(hints)
}
