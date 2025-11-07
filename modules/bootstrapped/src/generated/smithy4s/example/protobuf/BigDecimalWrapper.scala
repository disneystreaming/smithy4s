package smithy4s.example.protobuf

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bigdecimal
import smithy4s.schema.Schema.struct

final case class BigDecimalWrapper(bigDecimal: BigDecimal)

object BigDecimalWrapper extends ShapeTag.Companion[BigDecimalWrapper] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "BigDecimalWrapper")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("alloy.proto", "protoEnabled"), smithy4s.Document.obj()),
  )

  // constructor using the original order from the spec
  private def make(bigDecimal: BigDecimal): BigDecimalWrapper = BigDecimalWrapper(bigDecimal)

  implicit val schema: Schema[BigDecimalWrapper] = struct(
    bigdecimal.required[BigDecimalWrapper]("bigDecimal", _.bigDecimal),
  )(make).withId(id).addHints(hints)
}
