package smithy4s.example

import smithy4s.Blob
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

final case class DefaultTest(one: Int = 1, two: String = "test", three: List[String] = List(), four: List[String] = List(), seven: Document = smithy4s.Document.nullDoc, eight: Map[String, String] = Map(), five: Option[String] = None, six: Option[Int] = None, nine: Option[Short] = None, ten: Option[Double] = None, eleven: Option[Float] = None, twelve: Option[Long] = None, thirteen: Option[Timestamp] = None, fourteen: Option[Timestamp] = None, fifteen: Option[Timestamp] = None, sixteen: Option[Byte] = None, seventeen: Option[Blob] = None, eighteen: Option[Boolean] = None)

object DefaultTest extends ShapeTag.Companion[DefaultTest] {
  val id: ShapeId = ShapeId("smithy4s.example", "DefaultTest")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(one: Int, two: String, three: List[String], four: List[String], five: Option[String], six: Option[Int], seven: Document, eight: Map[String, String], nine: Option[Short], ten: Option[Double], eleven: Option[Float], twelve: Option[Long], thirteen: Option[Timestamp], fourteen: Option[Timestamp], fifteen: Option[Timestamp], sixteen: Option[Byte], seventeen: Option[Blob], eighteen: Option[Boolean]): DefaultTest = DefaultTest(one, two, three, four, seven, eight, five, six, nine, ten, eleven, twelve, thirteen, fourteen, fifteen, sixteen, seventeen, eighteen)

  implicit val schema: Schema[DefaultTest] = struct(
    int.field[DefaultTest]("one", _.one).addHints(smithy.api.Default(smithy4s.Document.fromDouble(1.0d))),
    string.field[DefaultTest]("two", _.two).addHints(smithy.api.Default(smithy4s.Document.fromString("test"))),
    StringList.underlyingSchema.field[DefaultTest]("three", _.three).addHints(smithy.api.Default(smithy4s.Document.array())),
    StringList.underlyingSchema.field[DefaultTest]("four", _.four).addHints(smithy.api.Default(smithy4s.Document.nullDoc)),
    string.optional[DefaultTest]("five", _.five).addHints(smithy.api.Default(smithy4s.Document.nullDoc)),
    int.optional[DefaultTest]("six", _.six).addHints(smithy.api.Default(smithy4s.Document.nullDoc)),
    document.field[DefaultTest]("seven", _.seven).addHints(smithy.api.Default(smithy4s.Document.nullDoc)),
    DefaultStringMap.underlyingSchema.field[DefaultTest]("eight", _.eight).addHints(smithy.api.Default(smithy4s.Document.nullDoc)),
    short.optional[DefaultTest]("nine", _.nine).addHints(smithy.api.Default(smithy4s.Document.nullDoc)),
    double.optional[DefaultTest]("ten", _.ten).addHints(smithy.api.Default(smithy4s.Document.nullDoc)),
    float.optional[DefaultTest]("eleven", _.eleven).addHints(smithy.api.Default(smithy4s.Document.nullDoc)),
    long.optional[DefaultTest]("twelve", _.twelve).addHints(smithy.api.Default(smithy4s.Document.nullDoc)),
    timestamp.optional[DefaultTest]("thirteen", _.thirteen).addHints(smithy.api.Default(smithy4s.Document.nullDoc)),
    timestamp.optional[DefaultTest]("fourteen", _.fourteen).addHints(smithy.api.TimestampFormat.HTTP_DATE.widen, smithy.api.Default(smithy4s.Document.nullDoc)),
    timestamp.optional[DefaultTest]("fifteen", _.fifteen).addHints(smithy.api.TimestampFormat.DATE_TIME.widen, smithy.api.Default(smithy4s.Document.nullDoc)),
    byte.optional[DefaultTest]("sixteen", _.sixteen).addHints(smithy.api.Default(smithy4s.Document.nullDoc)),
    bytes.optional[DefaultTest]("seventeen", _.seventeen).addHints(smithy.api.Default(smithy4s.Document.nullDoc)),
    boolean.optional[DefaultTest]("eighteen", _.eighteen).addHints(smithy.api.Default(smithy4s.Document.nullDoc)),
  )(make).withId(id).addHints(hints)
}
