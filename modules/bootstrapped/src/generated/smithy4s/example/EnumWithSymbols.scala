package smithy4s.example

import _root_.smithy4s.Enumeration
import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.EnumTag
import _root_.smithy4s.schema.Schema.enumeration

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
  val id: ShapeId = ShapeId("smithy4s.example", "EnumWithSymbols")

  val hints: Hints = Hints.empty

  case object FooFooFoo extends EnumWithSymbols("foo:foo:foo", "FooFooFoo", 0, Hints())
  case object BarBarBar extends EnumWithSymbols("bar:bar:bar", "BarBarBar", 1, Hints())
  case object Value2 extends EnumWithSymbols("_", "Value2", 2, Hints())

  val values: List[EnumWithSymbols] = List(
    FooFooFoo,
    BarBarBar,
    Value2,
  )
  val tag: EnumTag[EnumWithSymbols] = EnumTag.ClosedStringEnum
  implicit val schema: Schema[EnumWithSymbols] = enumeration(tag, values).withId(id).addHints(hints)
}
