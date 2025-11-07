package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.timestamp
import smithy4s.time.Timestamp

final case class HeadersStruct(str: Option[String] = None, int: Option[Int] = None, ts1: Option[Timestamp] = None, ts2: Option[Timestamp] = None, ts3: Option[Timestamp] = None, ts4: Option[Timestamp] = None, b: Option[Boolean] = None, sl: Option[List[String]] = None, ie: Option[Numbers] = None, on: Option[OpenNums] = None, ons: Option[OpenNumsStr] = None, slm: Option[Map[String, String]] = None)

object HeadersStruct extends ShapeTag.Companion[HeadersStruct] {
  val id: ShapeId = ShapeId("smithy4s.example", "HeadersStruct")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(str: Option[String], int: Option[Int], ts1: Option[Timestamp], ts2: Option[Timestamp], ts3: Option[Timestamp], ts4: Option[Timestamp], b: Option[Boolean], sl: Option[List[String]], ie: Option[Numbers], on: Option[OpenNums], ons: Option[OpenNumsStr], slm: Option[Map[String, String]]): HeadersStruct = HeadersStruct(str, int, ts1, ts2, ts3, ts4, b, sl, ie, on, ons, slm)

  implicit val schema: Schema[HeadersStruct] = struct(
    string.optional[HeadersStruct]("str", _.str).addHints(Hints.dynamic(ShapeId("smithy.api", "httpHeader"), smithy4s.Document.fromString("str"))),
    int.optional[HeadersStruct]("int", _.int).addHints(Hints.dynamic(ShapeId("smithy.api", "httpHeader"), smithy4s.Document.fromString("int"))),
    timestamp.optional[HeadersStruct]("ts1", _.ts1).addHints(Hints.dynamic(ShapeId("smithy.api", "httpHeader"), smithy4s.Document.fromString("ts1"))),
    timestamp.optional[HeadersStruct]("ts2", _.ts2).addHints(Hints.dynamic(ShapeId("smithy.api", "httpHeader"), smithy4s.Document.fromString("ts2")), Hints.dynamic(ShapeId("smithy.api", "timestampFormat"), smithy4s.Document.fromString("date-time"))),
    timestamp.optional[HeadersStruct]("ts3", _.ts3).addHints(Hints.dynamic(ShapeId("smithy.api", "httpHeader"), smithy4s.Document.fromString("ts3")), Hints.dynamic(ShapeId("smithy.api", "timestampFormat"), smithy4s.Document.fromString("epoch-seconds"))),
    timestamp.optional[HeadersStruct]("ts4", _.ts4).addHints(Hints.dynamic(ShapeId("smithy.api", "httpHeader"), smithy4s.Document.fromString("ts4")), Hints.dynamic(ShapeId("smithy.api", "timestampFormat"), smithy4s.Document.fromString("http-date"))),
    boolean.optional[HeadersStruct]("b", _.b).addHints(Hints.dynamic(ShapeId("smithy.api", "httpHeader"), smithy4s.Document.fromString("b"))),
    StringList.underlyingSchema.optional[HeadersStruct]("sl", _.sl).addHints(Hints.dynamic(ShapeId("smithy.api", "httpHeader"), smithy4s.Document.fromString("sl"))),
    Numbers.schema.optional[HeadersStruct]("ie", _.ie).addHints(Hints.dynamic(ShapeId("smithy.api", "httpHeader"), smithy4s.Document.fromString("nums"))),
    OpenNums.schema.optional[HeadersStruct]("on", _.on).addHints(Hints.dynamic(ShapeId("smithy.api", "httpHeader"), smithy4s.Document.fromString("openNums"))),
    OpenNumsStr.schema.optional[HeadersStruct]("ons", _.ons).addHints(Hints.dynamic(ShapeId("smithy.api", "httpHeader"), smithy4s.Document.fromString("openNumsStr"))),
    StringMap.underlyingSchema.optional[HeadersStruct]("slm", _.slm).addHints(Hints.dynamic(ShapeId("smithy.api", "httpPrefixHeaders"), smithy4s.Document.fromString("foo-"))),
  )(make).withId(id).addHints(hints)
}
