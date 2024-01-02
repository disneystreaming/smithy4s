package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.stringEnumeration

sealed abstract class EnumWithSymbols(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = EnumWithSymbols
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = EnumWithSymbols
  @inline final def widen: EnumWithSymbols = this
}
object EnumWithSymbols extends Enumeration[EnumWithSymbols] with ShapeTag.Companion[EnumWithSymbols] {
  val id: ShapeId = ShapeId("smithy4s.example", "EnumWithSymbols")

  val hints: Hints = Hints.empty

  case object FooFooFoo extends EnumWithSymbols("FooFooFoo", "foo:foo:foo", 0, Hints())
  case object BarBarBar extends EnumWithSymbols("BarBarBar", "bar:bar:bar", 1, Hints())
  case object Value2 extends EnumWithSymbols("Value2", "_", 2, Hints())

  val values: List[EnumWithSymbols] = List(
    FooFooFoo,
    BarBarBar,
    Value2,
  )
  implicit val schema: Schema[EnumWithSymbols] = stringEnumeration(values).withId(id).addHints(hints)
}
