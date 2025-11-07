package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.double
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.timestamp
import smithy4s.time.Timestamp

final case class Queries(str: Option[String] = None, int: Option[Int] = None, ts1: Option[Timestamp] = None, ts2: Option[Timestamp] = None, ts3: Option[Timestamp] = None, ts4: Option[Timestamp] = None, b: Option[Boolean] = None, sl: Option[List[String]] = None, ie: Option[Numbers] = None, on: Option[OpenNums] = None, ons: Option[OpenNumsStr] = None, dbl: Option[Double] = None, slm: Option[Map[String, String]] = None)

object Queries extends ShapeTag.Companion[Queries] {
  val id: ShapeId = ShapeId("smithy4s.example", "Queries")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(str: Option[String], int: Option[Int], ts1: Option[Timestamp], ts2: Option[Timestamp], ts3: Option[Timestamp], ts4: Option[Timestamp], b: Option[Boolean], sl: Option[List[String]], ie: Option[Numbers], on: Option[OpenNums], ons: Option[OpenNumsStr], dbl: Option[Double], slm: Option[Map[String, String]]): Queries = Queries(str, int, ts1, ts2, ts3, ts4, b, sl, ie, on, ons, dbl, slm)

  implicit val schema: Schema[Queries] = struct(
    string.optional[Queries]("str", _.str).addHints(Hints.dynamic(ShapeId("smithy.api", "httpQuery"), smithy4s.Document.fromString("str"))),
    int.optional[Queries]("int", _.int).addHints(Hints.dynamic(ShapeId("smithy.api", "httpQuery"), smithy4s.Document.fromString("int"))),
    timestamp.optional[Queries]("ts1", _.ts1).addHints(Hints.dynamic(ShapeId("smithy.api", "httpQuery"), smithy4s.Document.fromString("ts1"))),
    timestamp.optional[Queries]("ts2", _.ts2).addHints(Hints.dynamic(ShapeId("smithy.api", "httpQuery"), smithy4s.Document.fromString("ts2")), Hints.dynamic(ShapeId("smithy.api", "timestampFormat"), smithy4s.Document.fromString("date-time"))),
    timestamp.optional[Queries]("ts3", _.ts3).addHints(Hints.dynamic(ShapeId("smithy.api", "httpQuery"), smithy4s.Document.fromString("ts3")), Hints.dynamic(ShapeId("smithy.api", "timestampFormat"), smithy4s.Document.fromString("epoch-seconds"))),
    timestamp.optional[Queries]("ts4", _.ts4).addHints(Hints.dynamic(ShapeId("smithy.api", "httpQuery"), smithy4s.Document.fromString("ts4")), Hints.dynamic(ShapeId("smithy.api", "timestampFormat"), smithy4s.Document.fromString("http-date"))),
    boolean.optional[Queries]("b", _.b).addHints(Hints.dynamic(ShapeId("smithy.api", "httpQuery"), smithy4s.Document.fromString("b"))),
    StringList.underlyingSchema.optional[Queries]("sl", _.sl).addHints(Hints.dynamic(ShapeId("smithy.api", "httpQuery"), smithy4s.Document.fromString("sl"))),
    Numbers.schema.optional[Queries]("ie", _.ie).addHints(Hints.dynamic(ShapeId("smithy.api", "httpQuery"), smithy4s.Document.fromString("nums"))),
    OpenNums.schema.optional[Queries]("on", _.on).addHints(Hints.dynamic(ShapeId("smithy.api", "httpQuery"), smithy4s.Document.fromString("openNums"))),
    OpenNumsStr.schema.optional[Queries]("ons", _.ons).addHints(Hints.dynamic(ShapeId("smithy.api", "httpQuery"), smithy4s.Document.fromString("openNumsStr"))),
    double.validated(smithy.api.Range(min = Some(scala.math.BigDecimal(0.0)), max = Some(scala.math.BigDecimal(100.0)))).optional[Queries]("dbl", _.dbl).addHints(Hints.dynamic(ShapeId("smithy.api", "httpQuery"), smithy4s.Document.fromString("dbl"))),
    StringMap.underlyingSchema.optional[Queries]("slm", _.slm).addHints(Hints.dynamic(ShapeId("smithy.api", "httpQueryParams"), smithy4s.Document.obj())),
  )(make).withId(id).addHints(hints)
}
