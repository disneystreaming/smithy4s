package smithy4s.example

import smithy.api.Box
import smithy.api.Default
import smithy.api.TimestampFormat
import smithy4s.ByteArray
import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Timestamp
import smithy4s.schema.FieldLens
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
object DefaultTest extends ShapeTag.$Companion[DefaultTest] {
  val $id: ShapeId = ShapeId("smithy4s.example", "DefaultTest")

  val $hints: Hints = Hints.empty

  val one: FieldLens[DefaultTest, Int] = int.required[DefaultTest]("one", _.one, n => c => c.copy(one = n)).addHints(Default(smithy4s.Document.fromDouble(1.0d)))
  val two: FieldLens[DefaultTest, String] = string.required[DefaultTest]("two", _.two, n => c => c.copy(two = n)).addHints(Default(smithy4s.Document.fromString("test")))
  val three: FieldLens[DefaultTest, List[String]] = StringList.underlyingSchema.required[DefaultTest]("three", _.three, n => c => c.copy(three = n)).addHints(Default(smithy4s.Document.array()))
  val four: FieldLens[DefaultTest, List[String]] = StringList.underlyingSchema.required[DefaultTest]("four", _.four, n => c => c.copy(four = n)).addHints(Default(smithy4s.Document.array()))
  val five: FieldLens[DefaultTest, String] = string.required[DefaultTest]("five", _.five, n => c => c.copy(five = n)).addHints(Default(smithy4s.Document.fromString("")))
  val six: FieldLens[DefaultTest, Int] = int.required[DefaultTest]("six", _.six, n => c => c.copy(six = n)).addHints(Box(), Default(smithy4s.Document.fromDouble(0.0d)))
  val seven: FieldLens[DefaultTest, Document] = document.required[DefaultTest]("seven", _.seven, n => c => c.copy(seven = n)).addHints(Default(smithy4s.Document.nullDoc))
  val eight: FieldLens[DefaultTest, Map[String, String]] = DefaultStringMap.underlyingSchema.required[DefaultTest]("eight", _.eight, n => c => c.copy(eight = n)).addHints(Default(smithy4s.Document.obj()))
  val nine: FieldLens[DefaultTest, Short] = short.required[DefaultTest]("nine", _.nine, n => c => c.copy(nine = n)).addHints(Box(), Default(smithy4s.Document.fromDouble(0.0d)))
  val ten: FieldLens[DefaultTest, Double] = double.required[DefaultTest]("ten", _.ten, n => c => c.copy(ten = n)).addHints(Box(), Default(smithy4s.Document.fromDouble(0.0d)))
  val eleven: FieldLens[DefaultTest, Float] = float.required[DefaultTest]("eleven", _.eleven, n => c => c.copy(eleven = n)).addHints(Box(), Default(smithy4s.Document.fromDouble(0.0d)))
  val twelve: FieldLens[DefaultTest, Long] = long.required[DefaultTest]("twelve", _.twelve, n => c => c.copy(twelve = n)).addHints(Box(), Default(smithy4s.Document.fromDouble(0.0d)))
  val thirteen: FieldLens[DefaultTest, Timestamp] = timestamp.required[DefaultTest]("thirteen", _.thirteen, n => c => c.copy(thirteen = n)).addHints(Default(smithy4s.Document.fromDouble(0.0d)))
  val fourteen: FieldLens[DefaultTest, Timestamp] = timestamp.required[DefaultTest]("fourteen", _.fourteen, n => c => c.copy(fourteen = n)).addHints(TimestampFormat.HTTP_DATE.widen, Default(smithy4s.Document.fromString("Thu, 01 Jan 1970 00:00:00 GMT")))
  val fifteen: FieldLens[DefaultTest, Timestamp] = timestamp.required[DefaultTest]("fifteen", _.fifteen, n => c => c.copy(fifteen = n)).addHints(TimestampFormat.DATE_TIME.widen, Default(smithy4s.Document.fromString("1970-01-01T00:00:00.00Z")))
  val sixteen: FieldLens[DefaultTest, Byte] = byte.required[DefaultTest]("sixteen", _.sixteen, n => c => c.copy(sixteen = n)).addHints(Box(), Default(smithy4s.Document.fromDouble(0.0d)))
  val seventeen: FieldLens[DefaultTest, ByteArray] = bytes.required[DefaultTest]("seventeen", _.seventeen, n => c => c.copy(seventeen = n)).addHints(Default(smithy4s.Document.array()))
  val eighteen: FieldLens[DefaultTest, Boolean] = boolean.required[DefaultTest]("eighteen", _.eighteen, n => c => c.copy(eighteen = n)).addHints(Box(), Default(smithy4s.Document.fromBoolean(false)))

  implicit val $schema: Schema[DefaultTest] = struct(
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
  }.withId($id).addHints($hints)
}
