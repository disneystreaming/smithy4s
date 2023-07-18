package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.struct

final case class ListPublishersOutput(publishers: List[PublisherId])
object ListPublishersOutput extends ShapeTag.Companion[ListPublishersOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "ListPublishersOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  )

  object Lenses {
    val publishers = Lens[ListPublishersOutput, List[PublisherId]](_.publishers)(n => a => a.copy(publishers = n))
  }

  implicit val schema: Schema[ListPublishersOutput] = struct(
    PublishersList.underlyingSchema.required[ListPublishersOutput]("publishers", _.publishers).addHints(smithy.api.Required()),
  ){
    ListPublishersOutput.apply
  }.withId(id).addHints(hints)
}
