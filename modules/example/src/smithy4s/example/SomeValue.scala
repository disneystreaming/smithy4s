package smithy4s.example

import smithy4s.Newtype
import smithy4s.syntax._

object SomeValue extends Newtype[String] {
  object T {
    val schema : smithy4s.Schema[String] = string
    implicit val staticSchema : schematic.Static[smithy4s.Schema[String]] = schematic.Static(schema)
  }
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "SomeValue")

  val schema : smithy4s.Schema[SomeValue] = bijection(T.schema, SomeValue(_), (_ : SomeValue).value)
  implicit val staticSchema : schematic.Static[smithy4s.Schema[SomeValue]] = schematic.Static(schema)
}