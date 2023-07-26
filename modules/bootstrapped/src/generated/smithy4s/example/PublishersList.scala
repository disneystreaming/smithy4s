package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list

object PublishersList extends Newtype[List[PublisherId]] {
  val underlyingSchema: Schema[List[PublisherId]] = list(PublisherId.schema)
  .withId(ShapeId("smithy4s.example", "PublishersList"))
  .addHints(
    Hints.empty
  )

  implicit val schema: Schema[PublishersList] = bijection(underlyingSchema, asBijection)
}
