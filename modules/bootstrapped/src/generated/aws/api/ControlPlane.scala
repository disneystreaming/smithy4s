package aws.api

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

/** Defines a service, resource, or operation as operating on the control plane. */
final case class ControlPlane()
object ControlPlane extends ShapeTag.Companion[ControlPlane] {
  val id: ShapeId = ShapeId("aws.api", "controlPlane")

  val hints: Hints = Hints(
    smithy.api.Documentation("Defines a service, resource, or operation as operating on the control plane."),
    smithy.api.Trait(selector = Some(":test(service, resource, operation)"), structurallyExclusive = None, conflicts = Some(List(smithy.api.NonEmptyString("aws.api#dataPlane"))), breakingChanges = None),
  )

  implicit val schema: Schema[ControlPlane] = constant(ControlPlane()).withId(id).addHints(hints)
}
