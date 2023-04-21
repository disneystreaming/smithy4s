package smithy4s.catz.testcases

import smithy4s.schema.Schema
import smithy4s.{Hints, ShapeId}
import smithy4s.schema.Schema.enumeration


sealed abstract class FooBar(val stringValue: String, val intValue: Int) extends smithy4s.Enumeration.Value {
  override type EnumType = FooBar

  override def enumeration: smithy4s.Enumeration[EnumType] = FooBar

  val name = stringValue
  val value = stringValue
  val hints = Hints.empty

}

object FooBar extends smithy4s.Enumeration[FooBar] with smithy4s.ShapeTag.Companion[FooBar] {
  case object Foo extends FooBar("foo", 0)

  case object Bar extends FooBar("neq", 1)


  override def id: ShapeId = ShapeId("smithy4s.example", "FooBar")

  override def hints: Hints = Hints.empty

  override def values: List[FooBar] = List(Foo, Bar)

  implicit val schema: Schema[FooBar] =
    enumeration[FooBar](List(Foo, Bar))
}
