package smithy4s.example

import smithy4s.schema.Schema._

sealed abstract class EnumWithSymbols(_value: String, _ordinal: Int) extends smithy4s.Enumeration.Value {
  override val value : String = _value
  override val ordinal: Int = _ordinal
  override val hints: smithy4s.Hints = smithy4s.Hints.empty
}
object EnumWithSymbols extends smithy4s.Enumeration[EnumWithSymbols] with smithy4s.ShapeTag.Companion[EnumWithSymbols] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "EnumWithSymbols")

  val hints : smithy4s.Hints = smithy4s.Hints(
    smithy.api.Enum(List(smithy.api.EnumDefinition(smithy.api.NonEmptyString("foo:foo:foo"), None, None, None, None), smithy.api.EnumDefinition(smithy.api.NonEmptyString("bar:bar:bar"), None, None, None, None), smithy.api.EnumDefinition(smithy.api.NonEmptyString("_"), None, None, None, None))),
  )

  case object FooFooFoo extends EnumWithSymbols("foo:foo:foo", 0)
  case object BarBarBar extends EnumWithSymbols("bar:bar:bar", 1)
  case object Value2 extends EnumWithSymbols("_", 2)

  val values: List[EnumWithSymbols] = List(
    FooFooFoo,
    BarBarBar,
    Value2,
  )
  implicit val schema: smithy4s.Schema[EnumWithSymbols] = enumeration(values).withId(id).addHints(hints)
}