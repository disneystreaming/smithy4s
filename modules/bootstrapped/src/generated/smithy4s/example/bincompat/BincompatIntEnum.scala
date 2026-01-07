package smithy4s.example.bincompat

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.intEnumeration

sealed abstract class BincompatIntEnum(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = BincompatIntEnum
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = BincompatIntEnum
  @inline final def widen: BincompatIntEnum = this
}
object BincompatIntEnum extends Enumeration[BincompatIntEnum] with ShapeTag.Companion[BincompatIntEnum] {
  val id: ShapeId = ShapeId("smithy4s.example.bincompat", "BincompatIntEnum")

  val hints: Hints = Hints.empty

  private object impl {
    case object A extends BincompatIntEnum("A", "A", 1, Hints.empty)
    case object B extends BincompatIntEnum("B", "B", 2, Hints.empty)
    case object C extends BincompatIntEnum("C", "C", 3, Hints.empty)
  }

  val A: BincompatIntEnum = impl.A
  val B: BincompatIntEnum = impl.B
  val C: BincompatIntEnum = impl.C

  val values: List[BincompatIntEnum] = List(
    A,
    B,
    C,
  )
  implicit val schema: Schema[BincompatIntEnum] = intEnumeration(values).withId(id).addHints(hints)
}
