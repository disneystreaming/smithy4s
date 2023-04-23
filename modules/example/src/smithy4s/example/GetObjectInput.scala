package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

/** Input for getting an Object
  * all fields are required
  * and are given through HTTP labels
  * See https://smithy.io/2.0/spec/http-bindings.html?highlight=httppayload#http-uri-label
  * @param key
  *   Sent in the URI label named "key".
  *   Key can also be seen as the filename
  *   It is always required for a GET operation
  * @param bucketName
  *   Sent in the URI label named "bucketName".
  */
final case class GetObjectInput(key: ObjectKey, bucketName: BucketName)
object GetObjectInput extends ShapeTag.Companion[GetObjectInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetObjectInput")

  val hints: Hints = Hints(
    smithy.api.Documentation("Input for getting an Object\nall fields are required\nand are given through HTTP labels\nSee https://smithy.io/2.0/spec/http-bindings.html?highlight=httppayload#http-uri-label"),
  )

  implicit val schema: Schema[GetObjectInput] = struct(
    ObjectKey.schema.required[GetObjectInput]("key", _.key).addHints(smithy.api.Required(), smithy.api.Documentation("Sent in the URI label named \"key\".\nKey can also be seen as the filename\nIt is always required for a GET operation"), smithy.api.HttpLabel()),
    BucketName.schema.required[GetObjectInput]("bucketName", _.bucketName).addHints(smithy.api.Required(), smithy.api.Documentation("Sent in the URI label named \"bucketName\"."), smithy.api.HttpLabel()),
  ){
    GetObjectInput.apply
  }.withId(id).addHints(hints)
}