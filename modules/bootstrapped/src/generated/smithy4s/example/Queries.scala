package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Timestamp
import smithy4s.optics.Lens
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.timestamp

final case class Queries(str: Option[String] = None, int: Option[Int] = None, ts1: Option[Timestamp] = None, ts2: Option[Timestamp] = None, ts3: Option[Timestamp] = None, ts4: Option[Timestamp] = None, b: Option[Boolean] = None, sl: Option[List[String]] = None, ie: Option[Numbers] = None, slm: Option[Map[String, String]] = None)
object Queries extends ShapeTag.Companion[Queries] {
  val id: ShapeId = ShapeId("smithy4s.example", "Queries")

  val hints: Hints = Hints.empty

  object Optics {
    val str = Lens[Queries, Option[String]](_.str)(n => a => a.copy(str = n))
    val int = Lens[Queries, Option[Int]](_.int)(n => a => a.copy(int = n))
    val ts1 = Lens[Queries, Option[Timestamp]](_.ts1)(n => a => a.copy(ts1 = n))
    val ts2 = Lens[Queries, Option[Timestamp]](_.ts2)(n => a => a.copy(ts2 = n))
    val ts3 = Lens[Queries, Option[Timestamp]](_.ts3)(n => a => a.copy(ts3 = n))
    val ts4 = Lens[Queries, Option[Timestamp]](_.ts4)(n => a => a.copy(ts4 = n))
    val b = Lens[Queries, Option[Boolean]](_.b)(n => a => a.copy(b = n))
    val sl = Lens[Queries, Option[List[String]]](_.sl)(n => a => a.copy(sl = n))
    val ie = Lens[Queries, Option[Numbers]](_.ie)(n => a => a.copy(ie = n))
    val slm = Lens[Queries, Option[Map[String, String]]](_.slm)(n => a => a.copy(slm = n))
  }

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
    StringMap.underlyingSchema.optional[Queries]("slm", _.slm).addHints(smithy.api.HttpQueryParams()),
  ){
    Queries.apply
  }.withId(id).addHints(hints)
}
