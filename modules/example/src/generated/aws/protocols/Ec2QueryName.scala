package aws.protocols

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.string

/** Indicates the serialized name of a structure member when that structure is
  * serialized for the input of an EC2 operation.
  */
object Ec2QueryName extends Newtype[String] {
  val id: ShapeId = ShapeId("aws.protocols", "ec2QueryName")
  val hints: Hints = Hints(
    smithy.api.Documentation("Indicates the serialized name of a structure member when that structure is\nserialized for the input of an EC2 operation."),
    smithy.api.Trait(selector = Some("structure > member"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints).validated(smithy.api.Pattern("^[a-zA-Z_][a-zA-Z_0-9-]*$"))
  implicit val schema: Schema[Ec2QueryName] = recursive(bijection(underlyingSchema, asBijection))
}
