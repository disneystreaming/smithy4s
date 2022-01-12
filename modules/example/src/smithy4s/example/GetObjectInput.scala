package smithy4s.example

import smithy4s.syntax._

case class GetObjectInput(key: ObjectKey, bucketName: BucketName)
object GetObjectInput {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "GetObjectInput")

  val hints : smithy4s.Hints = smithy4s.Hints(
    id,
  )

  val schema: smithy4s.Schema[GetObjectInput] = struct(
    ObjectKey.schema.required[GetObjectInput]("key", _.key).withHints(smithy.api.Required(), smithy.api.HttpLabel()),
    BucketName.schema.required[GetObjectInput]("bucketName", _.bucketName).withHints(smithy.api.Required(), smithy.api.HttpLabel()),
  ){
    GetObjectInput.apply
  }.withHints(hints)
  implicit val staticSchema : schematic.Static[smithy4s.Schema[GetObjectInput]] = schematic.Static(schema)
}