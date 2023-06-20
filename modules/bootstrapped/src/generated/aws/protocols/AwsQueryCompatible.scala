package aws.protocols

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

/** Enable backward compatibility when migrating from awsQuery to awsJson protocol */
final case class AwsQueryCompatible()
object AwsQueryCompatible extends ShapeTag.Companion[AwsQueryCompatible] {
  val id: ShapeId = ShapeId("aws.protocols", "awsQueryCompatible")

  val hints: Hints = Hints(
    smithy.api.Documentation("Enable backward compatibility when migrating from awsQuery to awsJson protocol"),
    smithy.api.Trait(selector = Some("service [trait|aws.protocols#awsJson1_0]"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[AwsQueryCompatible] = constant(AwsQueryCompatible()).withId(id).addHints(hints)
}
