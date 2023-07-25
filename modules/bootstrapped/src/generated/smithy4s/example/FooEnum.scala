package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

sealed abstract class FooEnum(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = FooEnum
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = FooEnum
  @inline final def widen: FooEnum = this
}
object FooEnum extends Enumeration[FooEnum] with ShapeTag.Companion[FooEnum] {
  val hints: Hints = Hints.empty

  case object FOO extends FooEnum("Foo", "FOO", 0, Hints())

  val values: List[FooEnum] = List(
    FOO,
  )
  val tag: EnumTag = EnumTag.StringEnum
  implicit val schema: Schema[FooEnum] = enumeration(tag, values).withId(ShapeId("smithy4s.example", "FooEnum")).addHints(hints)
}
