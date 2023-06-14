package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Timestamp
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.timestamp

final case class Headers(str: Option[String] = None, int: Option[Int] = None, ts1: Option[Timestamp] = None, ts2: Option[Timestamp] = None, ts3: Option[Timestamp] = None, ts4: Option[Timestamp] = None, b: Option[Boolean] = None, sl: Option[List[String]] = None, ie: Option[Numbers] = None, slm: Option[Map[String, String]] = None)
object Headers extends ShapeTag.Companion[Headers] {
  val id: ShapeId = ShapeId("smithy4s.example", "Headers")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[Headers] = struct(
    string.optional[Headers]("str", _.str).addHints(smithy.api.HttpHeader("str")),
    int.optional[Headers]("int", _.int).addHints(smithy.api.HttpHeader("int")),
    timestamp.optional[Headers]("ts1", _.ts1).addHints(smithy.api.HttpHeader("ts1")),
    timestamp.optional[Headers]("ts2", _.ts2).addHints(smithy.api.TimestampFormat.DATE_TIME.widen, smithy.api.HttpHeader("ts2")),
    timestamp.optional[Headers]("ts3", _.ts3).addHints(smithy.api.TimestampFormat.EPOCH_SECONDS.widen, smithy.api.HttpHeader("ts3")),
    timestamp.optional[Headers]("ts4", _.ts4).addHints(smithy.api.TimestampFormat.HTTP_DATE.widen, smithy.api.HttpHeader("ts4")),
    boolean.optional[Headers]("b", _.b).addHints(smithy.api.HttpHeader("b")),
    StringList.underlyingSchema.optional[Headers]("sl", _.sl).addHints(smithy.api.HttpHeader("sl")),
    Numbers.schema.optional[Headers]("ie", _.ie).addHints(smithy.api.HttpHeader("nums")),
    StringMap.underlyingSchema.optional[Headers]("slm", _.slm).addHints(smithy.api.HttpPrefixHeaders("foo-")),
  ){
    Headers.apply
  }.withId(id).addHints(hints)
}
