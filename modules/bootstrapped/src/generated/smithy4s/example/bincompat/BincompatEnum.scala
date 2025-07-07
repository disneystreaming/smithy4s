package smithy4s.example.bincompat

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

sealed abstract class BincompatEnum(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = BincompatEnum
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = BincompatEnum
  @inline final def widen: BincompatEnum = this
}
object BincompatEnum extends Enumeration[BincompatEnum] with ShapeTag.Companion[BincompatEnum] {
  val id: ShapeId = ShapeId("smithy4s.example.bincompat", "BincompatEnum")

  val hints: Hints = Hints.empty

  private object impl {
    case object A extends BincompatEnum("A", "A", 0, Hints.empty)
    case object B extends BincompatEnum("B", "B", 1, Hints.empty)
    case object C extends BincompatEnum("C", "C", 2, Hints.empty)
  }

  val A: BincompatEnum = impl.A
  val B: BincompatEnum = impl.B
  val C: BincompatEnum = impl.C

  val values: List[BincompatEnum] = List(
    A,
    B,
    C,
  )
  val tag: EnumTag[BincompatEnum] = EnumTag.ClosedStringEnum
  implicit val schema: Schema[BincompatEnum] = enumeration(tag, values).withId(id).addHints(hints)
}
