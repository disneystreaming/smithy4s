package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

sealed abstract class OpenInt(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = OpenInt
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = OpenInt
  @inline final def widen: OpenInt = this
}
object OpenInt extends Enumeration[OpenInt] with ShapeTag.Companion[OpenInt] {
  val id: ShapeId = ShapeId("smithy4s.example", "OpenInt")

  val hints: Hints = Hints(
    alloy.OpenEnum(),
  ).lazily

  case object FOO extends OpenInt("FOO", "FOO", 0, Hints.empty)
  case object BAR extends OpenInt("BAR", "BAR", 1, Hints.empty)
  final case class $Unknown(int: Int) extends OpenInt("$Unknown", "$Unknown", int, Hints.empty)

  val $unknown: Int => OpenInt = $Unknown(_)

  val values: List[OpenInt] = List(
    FOO,
    BAR,
  )
  val tag: EnumTag[OpenInt] = EnumTag.OpenIntEnum($unknown)
  implicit val schema: Schema[OpenInt] = enumeration(tag, values).withId(id).addHints(hints)
}
