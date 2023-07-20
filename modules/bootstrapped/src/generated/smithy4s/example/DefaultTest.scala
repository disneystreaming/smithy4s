package smithy4s.example

import smithy4s.ByteArray
import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Timestamp
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.byte
import smithy4s.schema.Schema.bytes
import smithy4s.schema.Schema.document
import smithy4s.schema.Schema.double
import smithy4s.schema.Schema.float
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.long
import smithy4s.schema.Schema.short
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.timestamp

final case class DefaultTest(one: Int = 1, two: String = "test", three: List[String] = List(), four: List[String] = List(), five: String = "", six: Int = 0, seven: Document = smithy4s.Document.nullDoc, eight: Map[String, String] = Map(), nine: Short = 0, ten: Double = 0.0d, eleven: Float = 0.0f, twelve: Long = 0L, thirteen: Timestamp = Timestamp(0, 0), fourteen: Timestamp = Timestamp(0, 0), fifteen: Timestamp = Timestamp(0, 0), sixteen: Byte = 0, seventeen: ByteArray = ByteArray(Array()), eighteen: Boolean = false)
object DefaultTest extends ShapeTag.Companion[DefaultTest] {
  val id: ShapeId = ShapeId("smithy4s.example", "DefaultTest")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[DefaultTest] = struct(
    int.required[DefaultTest]("one", _.one).addHints(smithy.api.Default(smithy4s.Document.fromDouble(1.0d))),
    string.required[DefaultTest]("two", _.two).addHints(smithy.api.Default(smithy4s.Document.fromString("test"))),
    StringList.underlyingSchema.required[DefaultTest]("three", _.three).addHints(smithy.api.Default(smithy4s.Document.array())),
    StringList.underlyingSchema.required[DefaultTest]("four", _.four).addHints(smithy.api.Default(smithy4s.Document.array())),
    string.required[DefaultTest]("five", _.five).addHints(smithy.api.Default(smithy4s.Document.fromString(""))),
    int.required[DefaultTest]("six", _.six).addHints(smithy.api.Box(), smithy.api.Default(smithy4s.Document.fromDouble(0.0d))),
    document.required[DefaultTest]("seven", _.seven).addHints(smithy.api.Default(smithy4s.Document.nullDoc)),
    DefaultStringMap.underlyingSchema.required[DefaultTest]("eight", _.eight).addHints(smithy.api.Default(smithy4s.Document.obj())),
    short.required[DefaultTest]("nine", _.nine).addHints(smithy.api.Box(), smithy.api.Default(smithy4s.Document.fromDouble(0.0d))),
    double.required[DefaultTest]("ten", _.ten).addHints(smithy.api.Box(), smithy.api.Default(smithy4s.Document.fromDouble(0.0d))),
    float.required[DefaultTest]("eleven", _.eleven).addHints(smithy.api.Box(), smithy.api.Default(smithy4s.Document.fromDouble(0.0d))),
    long.required[DefaultTest]("twelve", _.twelve).addHints(smithy.api.Box(), smithy.api.Default(smithy4s.Document.fromDouble(0.0d))),
    timestamp.required[DefaultTest]("thirteen", _.thirteen).addHints(smithy.api.Default(smithy4s.Document.fromDouble(0.0d))),
    timestamp.required[DefaultTest]("fourteen", _.fourteen).addHints(smithy.api.TimestampFormat.HTTP_DATE.widen, smithy.api.Default(smithy4s.Document.fromString("Thu, 01 Jan 1970 00:00:00 GMT"))),
    timestamp.required[DefaultTest]("fifteen", _.fifteen).addHints(smithy.api.TimestampFormat.DATE_TIME.widen, smithy.api.Default(smithy4s.Document.fromString("1970-01-01T00:00:00.00Z"))),
    byte.required[DefaultTest]("sixteen", _.sixteen).addHints(smithy.api.Box(), smithy.api.Default(smithy4s.Document.fromDouble(0.0d))),
    bytes.required[DefaultTest]("seventeen", _.seventeen).addHints(smithy.api.Default(smithy4s.Document.array())),
    boolean.required[DefaultTest]("eighteen", _.eighteen).addHints(smithy.api.Box(), smithy.api.Default(smithy4s.Document.fromBoolean(false))),
  ){
    DefaultTest.apply
  }.withId(id).addHints(hints)
}
