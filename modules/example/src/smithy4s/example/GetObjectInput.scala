package smithy4s.example

import smithy4s._
import smithy4s.schema.Schema._

case class GetObjectInput(key: ObjectKey, bucketName: BucketName)
object GetObjectInput extends ShapeTag.Companion[GetObjectInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetObjectInput")
  
  val hints : Hints = Hints.empty
  
  implicit val schema: Schema[GetObjectInput] = struct(
    ObjectKey.schema.required[GetObjectInput]("key", _.key).addHints(smithy.api.Required(), smithy.api.HttpLabel()),
    BucketName.schema.required[GetObjectInput]("bucketName", _.bucketName).addHints(smithy.api.Required(), smithy.api.HttpLabel()),
  ){
    GetObjectInput.apply
  }.withId(id).addHints(hints)
}