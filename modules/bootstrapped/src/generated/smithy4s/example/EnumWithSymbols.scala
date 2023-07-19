package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Prism
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
  val id: ShapeId = ShapeId("smithy4s.example", "EnumWithSymbols")

  val hints: Hints = Hints.empty

  object Optics {
    val FooFooFoo = Prism.partial[EnumWithSymbols, EnumWithSymbols.FooFooFoo.type]{ case EnumWithSymbols.FooFooFoo => EnumWithSymbols.FooFooFoo }(identity)
    val BarBarBar = Prism.partial[EnumWithSymbols, EnumWithSymbols.BarBarBar.type]{ case EnumWithSymbols.BarBarBar => EnumWithSymbols.BarBarBar }(identity)
    val Value2 = Prism.partial[EnumWithSymbols, EnumWithSymbols.Value2.type]{ case EnumWithSymbols.Value2 => EnumWithSymbols.Value2 }(identity)
  }

  case object FooFooFoo extends EnumWithSymbols("foo:foo:foo", "FooFooFoo", 0, Hints())
  case object BarBarBar extends EnumWithSymbols("bar:bar:bar", "BarBarBar", 1, Hints())
  case object Value2 extends EnumWithSymbols("_", "Value2", 2, Hints())

  val values: List[EnumWithSymbols] = List(
    FooFooFoo,
    BarBarBar,
    Value2,
  )
  val tag: EnumTag = EnumTag.StringEnum
  implicit val schema: Schema[EnumWithSymbols] = enumeration(tag, values).withId(id).addHints(hints)
}
