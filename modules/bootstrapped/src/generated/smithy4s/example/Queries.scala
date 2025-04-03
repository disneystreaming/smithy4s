package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Timestamp
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.double
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.timestamp

final case class Queries(str: Option[String] = None, int: Option[Int] = None, ts1: Option[Timestamp] = None, ts2: Option[Timestamp] = None, ts3: Option[Timestamp] = None, ts4: Option[Timestamp] = None, b: Option[Boolean] = None, sl: Option[List[String]] = None, ie: Option[Numbers] = None, on: Option[OpenNums] = None, ons: Option[OpenNumsStr] = None, dbl: Option[Double] = None, slm: Option[Map[String, String]] = None)

object Queries extends ShapeTag.Companion[Queries] {
  val id: ShapeId = ShapeId("smithy4s.example", "Queries")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(str: Option[String], int: Option[Int], ts1: Option[Timestamp], ts2: Option[Timestamp], ts3: Option[Timestamp], ts4: Option[Timestamp], b: Option[Boolean], sl: Option[List[String]], ie: Option[Numbers], on: Option[OpenNums], ons: Option[OpenNumsStr], dbl: Option[Double], slm: Option[Map[String, String]]): Queries = Queries(str, int, ts1, ts2, ts3, ts4, b, sl, ie, on, ons, dbl, slm)

  implicit val schema: Schema[Queries] = struct(
    string.optional[Queries]("str", _.str).addHints(smithy.api.HttpQuery("str")),
    int.optional[Queries]("int", _.int).addHints(smithy.api.HttpQuery("int")),
    timestamp.optional[Queries]("ts1", _.ts1).addHints(smithy.api.HttpQuery("ts1")),
    timestamp.optional[Queries]("ts2", _.ts2).addHints(smithy.api.TimestampFormat.DATE_TIME.widen, smithy.api.HttpQuery("ts2")),
    timestamp.optional[Queries]("ts3", _.ts3).addHints(smithy.api.TimestampFormat.EPOCH_SECONDS.widen, smithy.api.HttpQuery("ts3")),
    timestamp.optional[Queries]("ts4", _.ts4).addHints(smithy.api.TimestampFormat.HTTP_DATE.widen, smithy.api.HttpQuery("ts4")),
    boolean.optional[Queries]("b", _.b).addHints(smithy.api.HttpQuery("b")),
    StringList.underlyingSchema.optional[Queries]("sl", _.sl).addHints(smithy.api.HttpQuery("sl")),
    Numbers.schema.optional[Queries]("ie", _.ie).addHints(smithy.api.HttpQuery("nums")),
    OpenNums.schema.optional[Queries]("on", _.on).addHints(smithy.api.HttpQuery("openNums")),
    OpenNumsStr.schema.optional[Queries]("ons", _.ons).addHints(smithy.api.HttpQuery("openNumsStr")),
    double.validated(smithy.api.Range(min = Some(scala.math.BigDecimal(0.0)), max = Some(scala.math.BigDecimal(100.0)))).optional[Queries]("dbl", _.dbl).addHints(smithy.api.HttpQuery("dbl")),
    StringMap.underlyingSchema.optional[Queries]("slm", _.slm).addHints(smithy.api.HttpQueryParams()),
  )(make).withId(id).addHints(hints)
}
