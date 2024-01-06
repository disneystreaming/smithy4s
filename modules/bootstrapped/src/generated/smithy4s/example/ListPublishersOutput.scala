package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct

final case class ListPublishersOutput(publishers: List[PublisherId])

object ListPublishersOutput extends ShapeTag.Companion[ListPublishersOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "ListPublishersOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  )

  implicit val schema: Schema[ListPublishersOutput] = struct(
    PublishersList.underlyingSchema.required[ListPublishersOutput]("publishers", _.publishers),
  ){
    ListPublishersOutput.apply
  }.withId(id).addHints(hints)
}
