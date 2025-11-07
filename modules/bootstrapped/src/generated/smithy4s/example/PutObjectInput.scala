package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

/** A key and bucket is always required for putting a new file in a bucket */
final case class PutObjectInput(key: ObjectKey, bucketName: BucketName, data: String, foo: Option[LowHigh] = None, someValue: Option[SomeValue] = None)

object PutObjectInput extends ShapeTag.Companion[PutObjectInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "PutObjectInput")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "documentation"), smithy4s.Document.fromString("A key and bucket is always required for putting a new file in a bucket")),
  )

  // constructor using the original order from the spec
  private def make(key: ObjectKey, bucketName: BucketName, foo: Option[LowHigh], someValue: Option[SomeValue], data: String): PutObjectInput = PutObjectInput(key, bucketName, data, foo, someValue)

  implicit val schema: Schema[PutObjectInput] = struct(
    ObjectKey.schema.required[PutObjectInput]("key", _.key).addHints(Hints.dynamic(ShapeId("smithy.api", "httpLabel"), smithy4s.Document.obj())),
    BucketName.schema.required[PutObjectInput]("bucketName", _.bucketName).addHints(Hints.dynamic(ShapeId("smithy.api", "httpLabel"), smithy4s.Document.obj())),
    LowHigh.schema.optional[PutObjectInput]("foo", _.foo).addHints(Hints.dynamic(ShapeId("smithy.api", "httpHeader"), smithy4s.Document.fromString("X-Foo"))),
    SomeValue.schema.optional[PutObjectInput]("someValue", _.someValue).addHints(Hints.dynamic(ShapeId("smithy.api", "httpQuery"), smithy4s.Document.fromString("paramName"))),
    string.required[PutObjectInput]("data", _.data).addHints(Hints.dynamic(ShapeId("smithy.api", "httpPayload"), smithy4s.Document.obj())),
  )(make).withId(id).addHints(hints)
}
