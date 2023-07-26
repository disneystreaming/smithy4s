package smithy4s.example

import smithy.api.Documentation
import smithy.api.HttpHeader
import smithy.api.HttpLabel
import smithy.api.HttpPayload
import smithy.api.HttpQuery
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

/** A key and bucket is always required for putting a new file in a bucket */
final case class PutObjectInput(key: ObjectKey, bucketName: BucketName, data: String, foo: Option[LowHigh] = None, someValue: Option[SomeValue] = None)
object PutObjectInput extends ShapeTag.Companion[PutObjectInput] {

  val key = ObjectKey.schema.required[PutObjectInput]("key", _.key, n => c => c.copy(key = n)).addHints(HttpLabel(), Required())
  val bucketName = BucketName.schema.required[PutObjectInput]("bucketName", _.bucketName, n => c => c.copy(bucketName = n)).addHints(HttpLabel(), Required())
  val data = string.required[PutObjectInput]("data", _.data, n => c => c.copy(data = n)).addHints(HttpPayload(), Required())
  val foo = LowHigh.schema.optional[PutObjectInput]("foo", _.foo, n => c => c.copy(foo = n)).addHints(HttpHeader("X-Foo"))
  val someValue = SomeValue.schema.optional[PutObjectInput]("someValue", _.someValue, n => c => c.copy(someValue = n)).addHints(HttpQuery("paramName"))

  implicit val schema: Schema[PutObjectInput] = struct(
    key,
    bucketName,
    data,
    foo,
    someValue,
  ){
    PutObjectInput.apply
  }
  .withId(ShapeId("smithy4s.example", "PutObjectInput"))
  .addHints(
    Hints(
      Documentation("A key and bucket is always required for putting a new file in a bucket"),
    )
  )
}
