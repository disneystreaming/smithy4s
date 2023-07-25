package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Timestamp
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.timestamp

final case class Queries(str: Option[String] = None, int: Option[Int] = None, ts1: Option[Timestamp] = None, ts2: Option[Timestamp] = None, ts3: Option[Timestamp] = None, ts4: Option[Timestamp] = None, b: Option[Boolean] = None, sl: Option[List[String]] = None, ie: Option[Numbers] = None, slm: Option[Map[String, String]] = None)
object Queries extends ShapeTag.Companion[Queries] {
  val hints: Hints = Hints.empty

  val str = string.optional[Queries]("str", _.str).addHints(smithy.api.HttpQuery("str"))
  val int = smithy4s.schema.Schema.int.optional[Queries]("int", _.int).addHints(smithy.api.HttpQuery("int"))
  val ts1 = timestamp.optional[Queries]("ts1", _.ts1).addHints(smithy.api.HttpQuery("ts1"))
  val ts2 = timestamp.optional[Queries]("ts2", _.ts2).addHints(smithy.api.TimestampFormat.DATE_TIME.widen, smithy.api.HttpQuery("ts2"))
  val ts3 = timestamp.optional[Queries]("ts3", _.ts3).addHints(smithy.api.TimestampFormat.EPOCH_SECONDS.widen, smithy.api.HttpQuery("ts3"))
  val ts4 = timestamp.optional[Queries]("ts4", _.ts4).addHints(smithy.api.TimestampFormat.HTTP_DATE.widen, smithy.api.HttpQuery("ts4"))
  val b = boolean.optional[Queries]("b", _.b).addHints(smithy.api.HttpQuery("b"))
  val sl = StringList.underlyingSchema.optional[Queries]("sl", _.sl).addHints(smithy.api.HttpQuery("sl"))
  val ie = Numbers.schema.optional[Queries]("ie", _.ie).addHints(smithy.api.HttpQuery("nums"))
  val slm = StringMap.underlyingSchema.optional[Queries]("slm", _.slm).addHints(smithy.api.HttpQueryParams())

  implicit val schema: Schema[Queries] = struct(
    str,
    int,
    ts1,
    ts2,
    ts3,
    ts4,
    b,
    sl,
    ie,
    slm,
  ){
    Queries.apply
  }.withId(ShapeId("smithy4s.example", "Queries")).addHints(hints)
}
