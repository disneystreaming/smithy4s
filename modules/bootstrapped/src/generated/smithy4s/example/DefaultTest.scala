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
  val hints: Hints = Hints.empty

  val one = int.required[DefaultTest]("one", _.one).addHints(smithy.api.Default(smithy4s.Document.fromDouble(1.0d)))
  val two = string.required[DefaultTest]("two", _.two).addHints(smithy.api.Default(smithy4s.Document.fromString("test")))
  val three = StringList.underlyingSchema.required[DefaultTest]("three", _.three).addHints(smithy.api.Default(smithy4s.Document.array()))
  val four = StringList.underlyingSchema.required[DefaultTest]("four", _.four).addHints(smithy.api.Default(smithy4s.Document.array()))
  val five = string.required[DefaultTest]("five", _.five).addHints(smithy.api.Default(smithy4s.Document.fromString("")))
  val six = int.required[DefaultTest]("six", _.six).addHints(smithy.api.Box(), smithy.api.Default(smithy4s.Document.fromDouble(0.0d)))
  val seven = document.required[DefaultTest]("seven", _.seven).addHints(smithy.api.Default(smithy4s.Document.nullDoc))
  val eight = DefaultStringMap.underlyingSchema.required[DefaultTest]("eight", _.eight).addHints(smithy.api.Default(smithy4s.Document.obj()))
  val nine = short.required[DefaultTest]("nine", _.nine).addHints(smithy.api.Box(), smithy.api.Default(smithy4s.Document.fromDouble(0.0d)))
  val ten = double.required[DefaultTest]("ten", _.ten).addHints(smithy.api.Box(), smithy.api.Default(smithy4s.Document.fromDouble(0.0d)))
  val eleven = float.required[DefaultTest]("eleven", _.eleven).addHints(smithy.api.Box(), smithy.api.Default(smithy4s.Document.fromDouble(0.0d)))
  val twelve = long.required[DefaultTest]("twelve", _.twelve).addHints(smithy.api.Box(), smithy.api.Default(smithy4s.Document.fromDouble(0.0d)))
  val thirteen = timestamp.required[DefaultTest]("thirteen", _.thirteen).addHints(smithy.api.Default(smithy4s.Document.fromDouble(0.0d)))
  val fourteen = timestamp.required[DefaultTest]("fourteen", _.fourteen).addHints(smithy.api.TimestampFormat.HTTP_DATE.widen, smithy.api.Default(smithy4s.Document.fromString("Thu, 01 Jan 1970 00:00:00 GMT")))
  val fifteen = timestamp.required[DefaultTest]("fifteen", _.fifteen).addHints(smithy.api.TimestampFormat.DATE_TIME.widen, smithy.api.Default(smithy4s.Document.fromString("1970-01-01T00:00:00.00Z")))
  val sixteen = byte.required[DefaultTest]("sixteen", _.sixteen).addHints(smithy.api.Box(), smithy.api.Default(smithy4s.Document.fromDouble(0.0d)))
  val seventeen = bytes.required[DefaultTest]("seventeen", _.seventeen).addHints(smithy.api.Default(smithy4s.Document.array()))
  val eighteen = boolean.required[DefaultTest]("eighteen", _.eighteen).addHints(smithy.api.Box(), smithy.api.Default(smithy4s.Document.fromBoolean(false)))

  implicit val schema: Schema[DefaultTest] = struct(
    one,
    two,
    three,
    four,
    five,
    six,
    seven,
    eight,
    nine,
    ten,
    eleven,
    twelve,
    thirteen,
    fourteen,
    fifteen,
    sixteen,
    seventeen,
    eighteen,
  ){
    DefaultTest.apply
  }.withId(ShapeId("smithy4s.example", "DefaultTest")).addHints(hints)
}
