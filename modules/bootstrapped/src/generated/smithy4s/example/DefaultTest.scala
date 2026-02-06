package smithy4s.example

import smithy4s.Blob
import smithy4s.Document
import smithy4s.Hints
import smithy4s.Nullable
import smithy4s.Nullable.Null
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
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
import smithy4s.time.Timestamp

final case class DefaultTest(one: Int = 1, two: String = "test", three: List[String] = List(), nineteen: Nullable[Int] = Null, twenty: Nullable[Int] = Null, four: Option[List[String]] = None, five: Option[String] = None, six: Option[Int] = None, seven: Option[Document] = None, eight: Option[Map[String, String]] = None, nine: Option[Short] = None, ten: Option[Double] = None, eleven: Option[Float] = None, twelve: Option[Long] = None, thirteen: Option[Timestamp] = None, fourteen: Option[Timestamp] = None, fifteen: Option[Timestamp] = None, sixteen: Option[Byte] = None, seventeen: Option[Blob] = None, eighteen: Option[Boolean] = None)

object DefaultTest extends ShapeTag.Companion[DefaultTest] {
  val id: ShapeId = ShapeId("smithy4s.example", "DefaultTest")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(one: Int, two: String, three: List[String], four: Option[List[String]], five: Option[String], six: Option[Int], seven: Option[Document], eight: Option[Map[String, String]], nine: Option[Short], ten: Option[Double], eleven: Option[Float], twelve: Option[Long], thirteen: Option[Timestamp], fourteen: Option[Timestamp], fifteen: Option[Timestamp], sixteen: Option[Byte], seventeen: Option[Blob], eighteen: Option[Boolean], nineteen: Nullable[Int], twenty: Nullable[Int]): DefaultTest = DefaultTest(one, two, three, nineteen, twenty, four, five, six, seven, eight, nine, ten, eleven, twelve, thirteen, fourteen, fifteen, sixteen, seventeen, eighteen)

  implicit val schema: Schema[DefaultTest] = struct(
    int.field[DefaultTest]("one", _.one).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.fromLong(1))),
    string.field[DefaultTest]("two", _.two).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.fromString("test"))),
    StringList.underlyingSchema.field[DefaultTest]("three", _.three).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.array())),
    StringList.underlyingSchema.optional[DefaultTest]("four", _.four).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.nullDoc)),
    string.optional[DefaultTest]("five", _.five).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.nullDoc)),
    int.optional[DefaultTest]("six", _.six).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.nullDoc)),
    document.optional[DefaultTest]("seven", _.seven).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.nullDoc)),
    DefaultStringMap.underlyingSchema.optional[DefaultTest]("eight", _.eight).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.nullDoc)),
    short.optional[DefaultTest]("nine", _.nine).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.nullDoc)),
    double.optional[DefaultTest]("ten", _.ten).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.nullDoc)),
    float.optional[DefaultTest]("eleven", _.eleven).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.nullDoc)),
    long.optional[DefaultTest]("twelve", _.twelve).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.nullDoc)),
    timestamp.optional[DefaultTest]("thirteen", _.thirteen).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.nullDoc)),
    timestamp.optional[DefaultTest]("fourteen", _.fourteen).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.nullDoc), Hints.dynamic(ShapeId("smithy.api", "timestampFormat"), smithy4s.Document.fromString("http-date"))),
    timestamp.optional[DefaultTest]("fifteen", _.fifteen).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.nullDoc), Hints.dynamic(ShapeId("smithy.api", "timestampFormat"), smithy4s.Document.fromString("date-time"))),
    byte.optional[DefaultTest]("sixteen", _.sixteen).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.nullDoc)),
    bytes.optional[DefaultTest]("seventeen", _.seventeen).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.nullDoc)),
    boolean.optional[DefaultTest]("eighteen", _.eighteen).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.nullDoc)),
    int.nullable.field[DefaultTest]("nineteen", _.nineteen).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.nullDoc)),
    int.nullable.required[DefaultTest]("twenty", _.twenty).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.nullDoc)),
  )(make).withId(id).addHints(hints)
}
