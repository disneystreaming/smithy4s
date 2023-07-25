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

final case class PathParams(str: String, int: Int, ts1: Timestamp, ts2: Timestamp, ts3: Timestamp, ts4: Timestamp, b: Boolean, ie: Numbers)
object PathParams extends ShapeTag.Companion[PathParams] {
  val id: ShapeId = ShapeId("smithy4s.example", "PathParams")

  val hints: Hints = Hints.empty

  val str = string.required[PathParams]("str", _.str).addHints(smithy.api.HttpLabel(), smithy.api.Required())
  val int = int.required[PathParams]("int", _.int).addHints(smithy.api.HttpLabel(), smithy.api.Required())
  val ts1 = timestamp.required[PathParams]("ts1", _.ts1).addHints(smithy.api.HttpLabel(), smithy.api.Required())
  val ts2 = timestamp.required[PathParams]("ts2", _.ts2).addHints(smithy.api.TimestampFormat.DATE_TIME.widen, smithy.api.Required(), smithy.api.HttpLabel())
  val ts3 = timestamp.required[PathParams]("ts3", _.ts3).addHints(smithy.api.TimestampFormat.EPOCH_SECONDS.widen, smithy.api.Required(), smithy.api.HttpLabel())
  val ts4 = timestamp.required[PathParams]("ts4", _.ts4).addHints(smithy.api.TimestampFormat.HTTP_DATE.widen, smithy.api.Required(), smithy.api.HttpLabel())
  val b = boolean.required[PathParams]("b", _.b).addHints(smithy.api.HttpLabel(), smithy.api.Required())
  val ie = Numbers.schema.required[PathParams]("ie", _.ie).addHints(smithy.api.HttpLabel(), smithy.api.Required())

  implicit val schema: Schema[PathParams] = struct(
    str,
    int,
    ts1,
    ts2,
    ts3,
    ts4,
    b,
    ie,
  ){
    PathParams.apply
  }.withId(id).addHints(hints)
}
