package smithy4s.example

import smithy.api.HttpLabel
import smithy.api.Required
import smithy.api.TimestampFormat
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Timestamp
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.timestamp

final case class PathParams(str: String, int: Int, ts1: Timestamp, ts2: Timestamp, ts3: Timestamp, ts4: Timestamp, b: Boolean, ie: Numbers)
object PathParams extends ShapeTag.$Companion[PathParams] {
  val $id: ShapeId = ShapeId("smithy4s.example", "PathParams")

  val $hints: Hints = Hints.empty

  val str: FieldLens[PathParams, String] = string.required[PathParams]("str", _.str, n => c => c.copy(str = n)).addHints(HttpLabel(), Required())
  val int: FieldLens[PathParams, Int] = smithy4s.schema.Schema.int.required[PathParams]("int", _.int, n => c => c.copy(int = n)).addHints(HttpLabel(), Required())
  val ts1: FieldLens[PathParams, Timestamp] = timestamp.required[PathParams]("ts1", _.ts1, n => c => c.copy(ts1 = n)).addHints(HttpLabel(), Required())
  val ts2: FieldLens[PathParams, Timestamp] = timestamp.required[PathParams]("ts2", _.ts2, n => c => c.copy(ts2 = n)).addHints(TimestampFormat.DATE_TIME.widen, Required(), HttpLabel())
  val ts3: FieldLens[PathParams, Timestamp] = timestamp.required[PathParams]("ts3", _.ts3, n => c => c.copy(ts3 = n)).addHints(TimestampFormat.EPOCH_SECONDS.widen, Required(), HttpLabel())
  val ts4: FieldLens[PathParams, Timestamp] = timestamp.required[PathParams]("ts4", _.ts4, n => c => c.copy(ts4 = n)).addHints(TimestampFormat.HTTP_DATE.widen, Required(), HttpLabel())
  val b: FieldLens[PathParams, Boolean] = boolean.required[PathParams]("b", _.b, n => c => c.copy(b = n)).addHints(HttpLabel(), Required())
  val ie: FieldLens[PathParams, Numbers] = Numbers.$schema.required[PathParams]("ie", _.ie, n => c => c.copy(ie = n)).addHints(HttpLabel(), Required())

  implicit val $schema: Schema[PathParams] = struct(
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
  }.withId($id).addHints($hints)
}
