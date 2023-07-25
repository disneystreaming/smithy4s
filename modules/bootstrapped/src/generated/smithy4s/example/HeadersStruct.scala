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

final case class HeadersStruct(str: Option[String] = None, int: Option[Int] = None, ts1: Option[Timestamp] = None, ts2: Option[Timestamp] = None, ts3: Option[Timestamp] = None, ts4: Option[Timestamp] = None, b: Option[Boolean] = None, sl: Option[List[String]] = None, ie: Option[Numbers] = None, slm: Option[Map[String, String]] = None)
object HeadersStruct extends ShapeTag.Companion[HeadersStruct] {
  val id: ShapeId = ShapeId("smithy4s.example", "HeadersStruct")

  val hints: Hints = Hints.empty

  val str = string.optional[HeadersStruct]("str", _.str).addHints(smithy.api.HttpHeader("str"))
  val int = int.optional[HeadersStruct]("int", _.int).addHints(smithy.api.HttpHeader("int"))
  val ts1 = timestamp.optional[HeadersStruct]("ts1", _.ts1).addHints(smithy.api.HttpHeader("ts1"))
  val ts2 = timestamp.optional[HeadersStruct]("ts2", _.ts2).addHints(smithy.api.TimestampFormat.DATE_TIME.widen, smithy.api.HttpHeader("ts2"))
  val ts3 = timestamp.optional[HeadersStruct]("ts3", _.ts3).addHints(smithy.api.TimestampFormat.EPOCH_SECONDS.widen, smithy.api.HttpHeader("ts3"))
  val ts4 = timestamp.optional[HeadersStruct]("ts4", _.ts4).addHints(smithy.api.TimestampFormat.HTTP_DATE.widen, smithy.api.HttpHeader("ts4"))
  val b = boolean.optional[HeadersStruct]("b", _.b).addHints(smithy.api.HttpHeader("b"))
  val sl = StringList.underlyingSchema.optional[HeadersStruct]("sl", _.sl).addHints(smithy.api.HttpHeader("sl"))
  val ie = Numbers.schema.optional[HeadersStruct]("ie", _.ie).addHints(smithy.api.HttpHeader("nums"))
  val slm = StringMap.underlyingSchema.optional[HeadersStruct]("slm", _.slm).addHints(smithy.api.HttpPrefixHeaders("foo-"))

  implicit val schema: Schema[HeadersStruct] = struct(
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
    HeadersStruct.apply
  }.withId(id).addHints(hints)
}
