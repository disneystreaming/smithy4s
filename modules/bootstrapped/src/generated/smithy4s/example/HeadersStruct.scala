package smithy4s.example

import smithy.api.HttpHeader
import smithy.api.HttpPrefixHeaders
import smithy.api.TimestampFormat
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Timestamp
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.timestamp

final case class HeadersStruct(str: Option[String] = None, int: Option[Int] = None, ts1: Option[Timestamp] = None, ts2: Option[Timestamp] = None, ts3: Option[Timestamp] = None, ts4: Option[Timestamp] = None, b: Option[Boolean] = None, sl: Option[List[String]] = None, ie: Option[Numbers] = None, slm: Option[Map[String, String]] = None)
object HeadersStruct extends ShapeTag.Companion[HeadersStruct] {

  val str: FieldLens[HeadersStruct, Option[String]] = string.optional[HeadersStruct]("str", _.str, n => c => c.copy(str = n)).addHints(HttpHeader("str"))
  val int: FieldLens[HeadersStruct, Option[Int]] = smithy4s.schema.Schema.int.optional[HeadersStruct]("int", _.int, n => c => c.copy(int = n)).addHints(HttpHeader("int"))
  val ts1: FieldLens[HeadersStruct, Option[Timestamp]] = timestamp.optional[HeadersStruct]("ts1", _.ts1, n => c => c.copy(ts1 = n)).addHints(HttpHeader("ts1"))
  val ts2: FieldLens[HeadersStruct, Option[Timestamp]] = timestamp.optional[HeadersStruct]("ts2", _.ts2, n => c => c.copy(ts2 = n)).addHints(TimestampFormat.DATE_TIME.widen, HttpHeader("ts2"))
  val ts3: FieldLens[HeadersStruct, Option[Timestamp]] = timestamp.optional[HeadersStruct]("ts3", _.ts3, n => c => c.copy(ts3 = n)).addHints(TimestampFormat.EPOCH_SECONDS.widen, HttpHeader("ts3"))
  val ts4: FieldLens[HeadersStruct, Option[Timestamp]] = timestamp.optional[HeadersStruct]("ts4", _.ts4, n => c => c.copy(ts4 = n)).addHints(TimestampFormat.HTTP_DATE.widen, HttpHeader("ts4"))
  val b: FieldLens[HeadersStruct, Option[Boolean]] = boolean.optional[HeadersStruct]("b", _.b, n => c => c.copy(b = n)).addHints(HttpHeader("b"))
  val sl: FieldLens[HeadersStruct, Option[List[String]]] = StringList.underlyingSchema.optional[HeadersStruct]("sl", _.sl, n => c => c.copy(sl = n)).addHints(HttpHeader("sl"))
  val ie: FieldLens[HeadersStruct, Option[Numbers]] = Numbers.schema.optional[HeadersStruct]("ie", _.ie, n => c => c.copy(ie = n)).addHints(HttpHeader("nums"))
  val slm: FieldLens[HeadersStruct, Option[Map[String, String]]] = StringMap.underlyingSchema.optional[HeadersStruct]("slm", _.slm, n => c => c.copy(slm = n)).addHints(HttpPrefixHeaders("foo-"))

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
  }
  .withId(ShapeId("smithy4s.example", "HeadersStruct"))
}
