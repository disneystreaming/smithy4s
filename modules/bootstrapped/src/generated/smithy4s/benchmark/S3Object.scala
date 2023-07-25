package smithy4s.benchmark

import smithy4s.ByteArray
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bytes
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class S3Object(id: String, owner: String, attributes: Attributes, data: ByteArray)
object S3Object extends ShapeTag.Companion[S3Object] {
  val hints: Hints = Hints.empty

  val id = string.required[S3Object]("id", _.id).addHints(smithy.api.Required())
  val owner = string.required[S3Object]("owner", _.owner).addHints(smithy.api.Required())
  val attributes = Attributes.schema.required[S3Object]("attributes", _.attributes).addHints(smithy.api.Required())
  val data = bytes.required[S3Object]("data", _.data).addHints(smithy.api.Required())

  implicit val schema: Schema[S3Object] = struct(
    id,
    owner,
    attributes,
    data,
  ){
    S3Object.apply
  }.withId(ShapeId("smithy4s.benchmark", "S3Object")).addHints(hints)
}
