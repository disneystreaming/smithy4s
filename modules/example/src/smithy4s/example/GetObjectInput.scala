package smithy4s.example

import smithy4s.syntax._

case class GetObjectInput(key: ObjectKey, bucketName: BucketName)
object GetObjectInput extends smithy4s.ShapeTag.Companion[GetObjectInput] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "GetObjectInput")

  val hints : smithy4s.Hints = smithy4s.Hints.empty

  implicit val schema: smithy4s.Schema[GetObjectInput] = struct(
    ObjectKey.schema.required[GetObjectInput]("key", _.key).addHints(smithy.api.Required(), smithy.api.HttpLabel()),
    BucketName.schema.required[GetObjectInput]("bucketName", _.bucketName).addHints(smithy.api.Required(), smithy.api.HttpLabel()),
  ){
    GetObjectInput.apply
  }.withId(id).addHints(hints)
}