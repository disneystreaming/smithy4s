package aws.api

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

/** Defines a service, resource, or operation as operating on the data plane. */
final case class DataPlane()
object DataPlane extends ShapeTag.Companion[DataPlane] {
  val id: ShapeId = ShapeId("aws.api", "dataPlane")

  val hints: Hints = Hints(
    smithy.api.Documentation("Defines a service, resource, or operation as operating on the data plane."),
    smithy.api.Trait(selector = Some(":test(service, resource, operation)"), structurallyExclusive = None, conflicts = Some(List(smithy.api.NonEmptyString("aws.api#controlPlane"))), breakingChanges = None),
  )

  implicit val schema: Schema[DataPlane] = constant(DataPlane()).withId(id).addHints(hints)
}
