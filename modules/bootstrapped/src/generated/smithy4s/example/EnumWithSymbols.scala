package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

sealed abstract class EnumWithSymbols(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = EnumWithSymbols
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = EnumWithSymbols
  @inline final def widen: EnumWithSymbols = this
}
object EnumWithSymbols extends Enumeration[EnumWithSymbols] with ShapeTag.Companion[EnumWithSymbols] {
  case object FooFooFoo extends EnumWithSymbols("foo:foo:foo", "FooFooFoo", 0, Hints())
  case object BarBarBar extends EnumWithSymbols("bar:bar:bar", "BarBarBar", 1, Hints())
  case object Value2 extends EnumWithSymbols("_", "Value2", 2, Hints())

  val values: List[EnumWithSymbols] = List(
    FooFooFoo,
    BarBarBar,
    Value2,
  )
  val tag: EnumTag = EnumTag.StringEnum
  implicit val schema: Schema[EnumWithSymbols] = enumeration(tag, values)
  .withId(ShapeId("smithy4s.example", "EnumWithSymbols"))
}
