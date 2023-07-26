package smithy4s.benchmark

import smithy.api.Required
import smithy4s.ByteArray
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.bytes
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class S3Object(id: String, owner: String, attributes: Attributes, data: ByteArray)
object S3Object extends ShapeTag.$Companion[S3Object] {
  val $id: ShapeId = ShapeId("smithy4s.benchmark", "S3Object")

  val $hints: Hints = Hints.empty

  val id: FieldLens[S3Object, String] = string.required[S3Object]("id", _.id, n => c => c.copy(id = n)).addHints(Required())
  val owner: FieldLens[S3Object, String] = string.required[S3Object]("owner", _.owner, n => c => c.copy(owner = n)).addHints(Required())
  val attributes: FieldLens[S3Object, Attributes] = Attributes.$schema.required[S3Object]("attributes", _.attributes, n => c => c.copy(attributes = n)).addHints(Required())
  val data: FieldLens[S3Object, ByteArray] = bytes.required[S3Object]("data", _.data, n => c => c.copy(data = n)).addHints(Required())

  implicit val $schema: Schema[S3Object] = struct(
    id,
    owner,
    attributes,
    data,
  ){
    S3Object.apply
  }.withId($id).addHints($hints)
}
