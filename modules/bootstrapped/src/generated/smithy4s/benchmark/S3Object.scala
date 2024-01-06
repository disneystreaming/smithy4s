package smithy4s.benchmark

import _root_.smithy4s.Blob
import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.bytes
import smithy4s.schema.Schema.string

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
