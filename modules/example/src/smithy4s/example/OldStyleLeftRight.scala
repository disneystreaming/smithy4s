package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.enumeration

sealed abstract class OldStyleLeftRight(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = OldStyleLeftRight
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = OldStyleLeftRight
  @inline final def widen: OldStyleLeftRight = this
}
object OldStyleLeftRight extends Enumeration[OldStyleLeftRight] with ShapeTag.Companion[OldStyleLeftRight] {
  val id: ShapeId = ShapeId("smithy4s.example", "oldStyleLeftRight")

  val hints: Hints = Hints(
    smithy.api.Enum(List(smithy.api.EnumDefinition(value = smithy.api.NonEmptyString("left"), name = Some(smithy.api.EnumConstantBodyName("LEFT")), documentation = None, tags = None, deprecated = None), smithy.api.EnumDefinition(value = smithy.api.NonEmptyString("right"), name = Some(smithy.api.EnumConstantBodyName("RIGHT")), documentation = None, tags = None, deprecated = None))),
  )

  case object LEFT extends OldStyleLeftRight("left", "LEFT", 0, Hints())
  case object RIGHT extends OldStyleLeftRight("right", "RIGHT", 1, Hints())

  val values: List[OldStyleLeftRight] = List(
    LEFT,
    RIGHT,
  )
  implicit val schema: Schema[OldStyleLeftRight] = enumeration(values).withId(id).addHints(hints)
}