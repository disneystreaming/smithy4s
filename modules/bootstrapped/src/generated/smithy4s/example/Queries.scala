package smithy4s.example

import smithy.api.HttpQuery
import smithy.api.HttpQueryParams
import smithy.api.TimestampFormat
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

  val str = string.optional[Queries]("str", _.str, n => c => c.copy(str = n)).addHints(HttpQuery("str"))
  val int = smithy4s.schema.Schema.int.optional[Queries]("int", _.int, n => c => c.copy(int = n)).addHints(HttpQuery("int"))
  val ts1 = timestamp.optional[Queries]("ts1", _.ts1, n => c => c.copy(ts1 = n)).addHints(HttpQuery("ts1"))
  val ts2 = timestamp.optional[Queries]("ts2", _.ts2, n => c => c.copy(ts2 = n)).addHints(TimestampFormat.DATE_TIME.widen, HttpQuery("ts2"))
  val ts3 = timestamp.optional[Queries]("ts3", _.ts3, n => c => c.copy(ts3 = n)).addHints(TimestampFormat.EPOCH_SECONDS.widen, HttpQuery("ts3"))
  val ts4 = timestamp.optional[Queries]("ts4", _.ts4, n => c => c.copy(ts4 = n)).addHints(TimestampFormat.HTTP_DATE.widen, HttpQuery("ts4"))
  val b = boolean.optional[Queries]("b", _.b, n => c => c.copy(b = n)).addHints(HttpQuery("b"))
  val sl = StringList.underlyingSchema.optional[Queries]("sl", _.sl, n => c => c.copy(sl = n)).addHints(HttpQuery("sl"))
  val ie = Numbers.schema.optional[Queries]("ie", _.ie, n => c => c.copy(ie = n)).addHints(HttpQuery("nums"))
  val slm = StringMap.underlyingSchema.optional[Queries]("slm", _.slm, n => c => c.copy(slm = n)).addHints(HttpQueryParams())

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
  }
  .withId(ShapeId("smithy4s.example", "Queries"))
  .addHints(
    Hints.empty
  )
}
