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

final case class PathParams(str: String, int: Int, ts1: Timestamp, ts2: Timestamp, ts3: Timestamp, ts4: Timestamp, b: Boolean, ie: Numbers)
object PathParams extends ShapeTag.Companion[PathParams] {
  val id: ShapeId = ShapeId("smithy4s.example", "PathParams")

  val hints: Hints = Hints.empty

  object Optics {
    val str = Lens[PathParams, String](_.str)(n => a => a.copy(str = n))
    val int = Lens[PathParams, Int](_.int)(n => a => a.copy(int = n))
    val ts1 = Lens[PathParams, Timestamp](_.ts1)(n => a => a.copy(ts1 = n))
    val ts2 = Lens[PathParams, Timestamp](_.ts2)(n => a => a.copy(ts2 = n))
    val ts3 = Lens[PathParams, Timestamp](_.ts3)(n => a => a.copy(ts3 = n))
    val ts4 = Lens[PathParams, Timestamp](_.ts4)(n => a => a.copy(ts4 = n))
    val b = Lens[PathParams, Boolean](_.b)(n => a => a.copy(b = n))
    val ie = Lens[PathParams, Numbers](_.ie)(n => a => a.copy(ie = n))
  }

  implicit val schema: Schema[PathParams] = struct(
    string.required[PathParams]("str", _.str).addHints(smithy.api.HttpLabel(), smithy.api.Required()),
    int.required[PathParams]("int", _.int).addHints(smithy.api.HttpLabel(), smithy.api.Required()),
    timestamp.required[PathParams]("ts1", _.ts1).addHints(smithy.api.HttpLabel(), smithy.api.Required()),
    timestamp.required[PathParams]("ts2", _.ts2).addHints(smithy.api.TimestampFormat.DATE_TIME.widen, smithy.api.Required(), smithy.api.HttpLabel()),
    timestamp.required[PathParams]("ts3", _.ts3).addHints(smithy.api.TimestampFormat.EPOCH_SECONDS.widen, smithy.api.Required(), smithy.api.HttpLabel()),
    timestamp.required[PathParams]("ts4", _.ts4).addHints(smithy.api.TimestampFormat.HTTP_DATE.widen, smithy.api.Required(), smithy.api.HttpLabel()),
    boolean.required[PathParams]("b", _.b).addHints(smithy.api.HttpLabel(), smithy.api.Required()),
    Numbers.schema.required[PathParams]("ie", _.ie).addHints(smithy.api.HttpLabel(), smithy.api.Required()),
  ){
    PathParams.apply
  }.withId(id).addHints(hints)
}
