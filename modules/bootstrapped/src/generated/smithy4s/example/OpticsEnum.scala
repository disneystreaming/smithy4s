package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Prism
import smithy4s.schema.Schema.stringEnumeration

sealed abstract class OpticsEnum(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = OpticsEnum
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = OpticsEnum
  @inline final def widen: OpticsEnum = this
}
object OpticsEnum extends Enumeration[OpticsEnum] with ShapeTag.Companion[OpticsEnum] {
  val id: ShapeId = ShapeId("smithy4s.example", "OpticsEnum")

  val hints: Hints = Hints.empty

  object optics {
    val A: Prism[OpticsEnum, OpticsEnum.A.type] = Prism.partial[OpticsEnum, OpticsEnum.A.type]{ case OpticsEnum.A => OpticsEnum.A }(identity)
    val B: Prism[OpticsEnum, OpticsEnum.B.type] = Prism.partial[OpticsEnum, OpticsEnum.B.type]{ case OpticsEnum.B => OpticsEnum.B }(identity)
  }

  case object A extends OpticsEnum("A", "A", 0, Hints.empty)
  case object B extends OpticsEnum("B", "B", 1, Hints.empty)

  val values: List[OpticsEnum] = List(
    A,
    B,
  )
  implicit val schema: Schema[OpticsEnum] = stringEnumeration(values).withId(id).addHints(hints)
}
