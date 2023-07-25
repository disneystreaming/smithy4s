package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Prism
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

sealed abstract class OpticsEnum(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = OpticsEnum
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = OpticsEnum
  @inline final def widen: OpticsEnum = this
}
object OpticsEnum extends Enumeration[OpticsEnum] with ShapeTag.Companion[OpticsEnum] {
  val hints: Hints = Hints.empty

  object Optics {
    val A: Prism[OpticsEnum, OpticsEnum.A.type] = Prism.partial[OpticsEnum, OpticsEnum.A.type]{ case OpticsEnum.A => OpticsEnum.A }(identity)
    val B: Prism[OpticsEnum, OpticsEnum.B.type] = Prism.partial[OpticsEnum, OpticsEnum.B.type]{ case OpticsEnum.B => OpticsEnum.B }(identity)
  }

  case object A extends OpticsEnum("A", "A", 0, Hints())
  case object B extends OpticsEnum("B", "B", 1, Hints())

  val values: List[OpticsEnum] = List(
    A,
    B,
  )
  val tag: EnumTag = EnumTag.StringEnum
  implicit val schema: Schema[OpticsEnum] = enumeration(tag, values).withId(ShapeId("smithy4s.example", "OpticsEnum")).addHints(hints)
}
