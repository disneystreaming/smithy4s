package smithy4s.example

import smithy.api.Output
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class ListPublishersOutput(publishers: List[PublisherId])
object ListPublishersOutput extends ShapeTag.Companion[ListPublishersOutput] {

  val publishers = PublishersList.underlyingSchema.required[ListPublishersOutput]("publishers", _.publishers, n => c => c.copy(publishers = n)).addHints(Required())

  implicit val schema: Schema[ListPublishersOutput] = struct(
    publishers,
  ){
    ListPublishersOutput.apply
  }
  .withId(ShapeId("smithy4s.example", "ListPublishersOutput"))
  .addHints(
    Hints(
      Output(),
    )
  )
}
