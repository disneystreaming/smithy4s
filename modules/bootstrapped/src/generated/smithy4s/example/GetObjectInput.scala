package smithy4s.example

import smithy.api.Documentation
import smithy.api.HttpLabel
import smithy.api.Required
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
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

  val key: FieldLens[GetObjectInput, ObjectKey] = ObjectKey.schema.required[GetObjectInput]("key", _.key, n => c => c.copy(key = n)).addHints(Required(), Documentation("Sent in the URI label named \"key\".\nKey can also be seen as the filename\nIt is always required for a GET operation"), HttpLabel())
  val bucketName: FieldLens[GetObjectInput, BucketName] = BucketName.schema.required[GetObjectInput]("bucketName", _.bucketName, n => c => c.copy(bucketName = n)).addHints(Required(), Documentation("Sent in the URI label named \"bucketName\"."), HttpLabel())

  implicit val schema: Schema[GetObjectInput] = struct(
    key,
    bucketName,
  ){
    GetObjectInput.apply
  }
  .withId(ShapeId("smithy4s.example", "GetObjectInput"))
  .addHints(
    Documentation("Input for getting an Object\nall fields are required\nand are given through HTTP labels\nSee https://smithy.io/2.0/spec/http-bindings.html?highlight=httppayload#http-uri-label"),
  )
}
