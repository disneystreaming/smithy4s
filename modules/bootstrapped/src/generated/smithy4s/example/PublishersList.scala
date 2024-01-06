package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list

object PublishersList extends Newtype[List[PublisherId]] {
  val id: ShapeId = ShapeId("smithy4s.example", "PublishersList")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[List[PublisherId]] = list(PublisherId.schema).withId(id).addHints(hints)
  implicit val schema: Schema[PublishersList] = bijection(underlyingSchema, asBijection)
}
