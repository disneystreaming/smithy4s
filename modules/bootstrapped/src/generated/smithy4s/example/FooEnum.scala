package smithy4s.example

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.stringEnumeration

sealed abstract class FooEnum(_name: String, _stringValue: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = FooEnum
  override val name: String = _name
  override val stringValue: String = _stringValue
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = FooEnum
  @inline final def widen: FooEnum = this
}
object FooEnum extends Enumeration[FooEnum] with ShapeTag.Companion[FooEnum] {
  val id: ShapeId = ShapeId("smithy4s.example", "FooEnum")

  val hints: Hints = Hints.empty

  case object FOO extends FooEnum("FOO", "Foo", 0, Hints.empty)

  val values: List[FooEnum] = List(
    FOO,
  )
  implicit val schema: Schema[FooEnum] = stringEnumeration(values).withId(id).addHints(hints)
}
