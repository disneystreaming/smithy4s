package smithy4s.benchmark

import smithy4s.Blob
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bytes
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class S3Object(id: String, owner: String, attributes: Attributes, data: Blob)
object S3Object extends ShapeTag.Companion[S3Object] {
  val id: ShapeId = ShapeId("smithy4s.benchmark", "S3Object")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[S3Object] = struct(
    string.required[S3Object]("id", _.id),
    string.required[S3Object]("owner", _.owner),
    Attributes.schema.required[S3Object]("attributes", _.attributes),
    bytes.required[S3Object]("data", _.data),
  ){
    S3Object.apply
  }.withId(id).addHints(hints)
}
