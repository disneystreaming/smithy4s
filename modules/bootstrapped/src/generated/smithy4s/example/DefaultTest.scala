package smithy4s.example

import _root_.smithy4s.Blob
import _root_.smithy4s.Document
import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.Timestamp
import _root_.smithy4s.schema.Schema.struct
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
import smithy4s.schema.Schema.timestamp

final case class DefaultTest(one: Int = 1, two: String = "test", three: List[String] = List(), four: List[String] = List(), five: String = "", six: Int = 0, seven: Document = _root_.smithy4s.Document.nullDoc, eight: Map[String, String] = Map(), nine: Short = 0, ten: Double = 0.0d, eleven: Float = 0.0f, twelve: Long = 0L, thirteen: Timestamp = Timestamp(0, 0), fourteen: Timestamp = Timestamp(0, 0), fifteen: Timestamp = Timestamp(0, 0), sixteen: Byte = 0, seventeen: Blob = Blob.empty, eighteen: Boolean = false)

object DefaultTest extends ShapeTag.Companion[DefaultTest] {
  val id: ShapeId = ShapeId("smithy4s.example", "DefaultTest")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[DefaultTest] = struct(
    int.field[DefaultTest]("one", _.one).addHints(smithy.api.Default(_root_.smithy4s.Document.fromDouble(1.0d))),
    string.field[DefaultTest]("two", _.two).addHints(smithy.api.Default(_root_.smithy4s.Document.fromString("test"))),
    StringList.underlyingSchema.field[DefaultTest]("three", _.three).addHints(smithy.api.Default(_root_.smithy4s.Document.array())),
    StringList.underlyingSchema.field[DefaultTest]("four", _.four).addHints(smithy.api.Default(_root_.smithy4s.Document.array())),
    string.field[DefaultTest]("five", _.five).addHints(smithy.api.Default(_root_.smithy4s.Document.fromString(""))),
    int.field[DefaultTest]("six", _.six).addHints(smithy.api.Box(), smithy.api.Default(_root_.smithy4s.Document.fromDouble(0.0d))),
    document.field[DefaultTest]("seven", _.seven).addHints(smithy.api.Default(_root_.smithy4s.Document.nullDoc)),
    DefaultStringMap.underlyingSchema.field[DefaultTest]("eight", _.eight).addHints(smithy.api.Default(_root_.smithy4s.Document.obj())),
    short.field[DefaultTest]("nine", _.nine).addHints(smithy.api.Box(), smithy.api.Default(_root_.smithy4s.Document.fromDouble(0.0d))),
    double.field[DefaultTest]("ten", _.ten).addHints(smithy.api.Box(), smithy.api.Default(_root_.smithy4s.Document.fromDouble(0.0d))),
    float.field[DefaultTest]("eleven", _.eleven).addHints(smithy.api.Box(), smithy.api.Default(_root_.smithy4s.Document.fromDouble(0.0d))),
    long.field[DefaultTest]("twelve", _.twelve).addHints(smithy.api.Box(), smithy.api.Default(_root_.smithy4s.Document.fromDouble(0.0d))),
    timestamp.field[DefaultTest]("thirteen", _.thirteen).addHints(smithy.api.Default(_root_.smithy4s.Document.fromDouble(0.0d))),
    timestamp.field[DefaultTest]("fourteen", _.fourteen).addHints(smithy.api.TimestampFormat.HTTP_DATE.widen, smithy.api.Default(_root_.smithy4s.Document.fromString("Thu, 01 Jan 1970 00:00:00 GMT"))),
    timestamp.field[DefaultTest]("fifteen", _.fifteen).addHints(smithy.api.TimestampFormat.DATE_TIME.widen, smithy.api.Default(_root_.smithy4s.Document.fromString("1970-01-01T00:00:00.00Z"))),
    byte.field[DefaultTest]("sixteen", _.sixteen).addHints(smithy.api.Box(), smithy.api.Default(_root_.smithy4s.Document.fromDouble(0.0d))),
    bytes.field[DefaultTest]("seventeen", _.seventeen).addHints(smithy.api.Default(_root_.smithy4s.Document.array())),
    boolean.field[DefaultTest]("eighteen", _.eighteen).addHints(smithy.api.Box(), smithy.api.Default(_root_.smithy4s.Document.fromBoolean(false))),
  ){
    DefaultTest.apply
  }.withId(id).addHints(hints)
}
