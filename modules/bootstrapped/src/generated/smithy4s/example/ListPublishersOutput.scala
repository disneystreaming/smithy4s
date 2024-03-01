package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class ListPublishersOutput(publishers: List[PublisherId])

object ListPublishersOutput extends ShapeTag.Companion[ListPublishersOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "ListPublishersOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  ).lazily

  // constructor using the original order from the spec
  private def make(publishers: List[PublisherId]): ListPublishersOutput = ListPublishersOutput(publishers)

  implicit val schema: Schema[ListPublishersOutput] = struct(
    PublishersList.underlyingSchema.required[ListPublishersOutput]("publishers", _.publishers),
  ){
    make
  }.withId(id).addHints(hints)
}
