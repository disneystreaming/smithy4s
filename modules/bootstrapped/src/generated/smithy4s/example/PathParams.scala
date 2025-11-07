package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.timestamp
import smithy4s.time.Timestamp

final case class PathParams(str: String, int: Int, ts1: Timestamp, ts2: Timestamp, ts3: Timestamp, ts4: Timestamp, b: Boolean, ie: Numbers)

object PathParams extends ShapeTag.Companion[PathParams] {
  val id: ShapeId = ShapeId("smithy4s.example", "PathParams")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(str: String, int: Int, ts1: Timestamp, ts2: Timestamp, ts3: Timestamp, ts4: Timestamp, b: Boolean, ie: Numbers): PathParams = PathParams(str, int, ts1, ts2, ts3, ts4, b, ie)

  implicit val schema: Schema[PathParams] = struct(
    string.required[PathParams]("str", _.str).addHints(Hints.dynamic(ShapeId("smithy.api", "httpLabel"), smithy4s.Document.obj())),
    int.required[PathParams]("int", _.int).addHints(Hints.dynamic(ShapeId("smithy.api", "httpLabel"), smithy4s.Document.obj())),
    timestamp.required[PathParams]("ts1", _.ts1).addHints(Hints.dynamic(ShapeId("smithy.api", "httpLabel"), smithy4s.Document.obj())),
    timestamp.required[PathParams]("ts2", _.ts2).addHints(Hints.dynamic(ShapeId("smithy.api", "httpLabel"), smithy4s.Document.obj()), Hints.dynamic(ShapeId("smithy.api", "timestampFormat"), smithy4s.Document.fromString("date-time"))),
    timestamp.required[PathParams]("ts3", _.ts3).addHints(Hints.dynamic(ShapeId("smithy.api", "httpLabel"), smithy4s.Document.obj()), Hints.dynamic(ShapeId("smithy.api", "timestampFormat"), smithy4s.Document.fromString("epoch-seconds"))),
    timestamp.required[PathParams]("ts4", _.ts4).addHints(Hints.dynamic(ShapeId("smithy.api", "httpLabel"), smithy4s.Document.obj()), Hints.dynamic(ShapeId("smithy.api", "timestampFormat"), smithy4s.Document.fromString("http-date"))),
    boolean.required[PathParams]("b", _.b).addHints(Hints.dynamic(ShapeId("smithy.api", "httpLabel"), smithy4s.Document.obj())),
    Numbers.schema.required[PathParams]("ie", _.ie).addHints(Hints.dynamic(ShapeId("smithy.api", "httpLabel"), smithy4s.Document.obj())),
  )(make).withId(id).addHints(hints)
}
