package smithy4s.example.protobuf

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.string

object WrappedStringList extends Newtype[List[String]] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "WrappedStringList")
  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("alloy.proto", "protoWrapped"), smithy4s.Document.obj()),
  )
  val underlyingSchema: Schema[List[String]] = list(string).withId(id).addHints(hints)
  implicit val schema: Schema[WrappedStringList] = bijection(underlyingSchema, asBijection)
}
