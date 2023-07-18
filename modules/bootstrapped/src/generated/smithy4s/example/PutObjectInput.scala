package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

/** A key and bucket is always required for putting a new file in a bucket */
final case class PutObjectInput(key: ObjectKey, bucketName: BucketName, data: String, foo: Option[LowHigh] = None, someValue: Option[SomeValue] = None)
object PutObjectInput extends ShapeTag.Companion[PutObjectInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "PutObjectInput")

  val hints: Hints = Hints(
    smithy.api.Documentation("A key and bucket is always required for putting a new file in a bucket"),
  )

  object Lenses {
    val key = Lens[PutObjectInput, ObjectKey](_.key)(n => a => a.copy(key = n))
    val bucketName = Lens[PutObjectInput, BucketName](_.bucketName)(n => a => a.copy(bucketName = n))
    val data = Lens[PutObjectInput, String](_.data)(n => a => a.copy(data = n))
    val foo = Lens[PutObjectInput, Option[LowHigh]](_.foo)(n => a => a.copy(foo = n))
    val someValue = Lens[PutObjectInput, Option[SomeValue]](_.someValue)(n => a => a.copy(someValue = n))
  }

  implicit val schema: Schema[PutObjectInput] = struct(
    ObjectKey.schema.required[PutObjectInput]("key", _.key).addHints(smithy.api.HttpLabel(), smithy.api.Required()),
    BucketName.schema.required[PutObjectInput]("bucketName", _.bucketName).addHints(smithy.api.HttpLabel(), smithy.api.Required()),
    string.required[PutObjectInput]("data", _.data).addHints(smithy.api.HttpPayload(), smithy.api.Required()),
    LowHigh.schema.optional[PutObjectInput]("foo", _.foo).addHints(smithy.api.HttpHeader("X-Foo")),
    SomeValue.schema.optional[PutObjectInput]("someValue", _.someValue).addHints(smithy.api.HttpQuery("paramName")),
  ){
    PutObjectInput.apply
  }.withId(id).addHints(hints)
}
