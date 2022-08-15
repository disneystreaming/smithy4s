package smithy4s.example

import smithy4s.Schema
import smithy4s.Hints
import smithy4s.ShapeId
import smithy4s.schema.Schema.struct
import smithy4s.ShapeTag

case class GetObjectInput(key: ObjectKey, bucketName: BucketName)
object GetObjectInput extends ShapeTag.Companion[GetObjectInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetObjectInput")

  val hints : Hints = Hints.empty

  implicit val schema: Schema[GetObjectInput] = struct(
    ObjectKey.schema.required[GetObjectInput]("key", _.key).addHints(smithy.api.HttpLabel(), smithy.api.Required()),
    BucketName.schema.required[GetObjectInput]("bucketName", _.bucketName).addHints(smithy.api.HttpLabel(), smithy.api.Required()),
  ){
    GetObjectInput.apply
  }.withId(id).addHints(hints)
}