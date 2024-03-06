package smithy4s.example

import java.util.UUID
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bigdecimal
import smithy4s.schema.Schema.bigint
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.uuid

final case class Other(bigDecimal: BigDecimal, bigInteger: BigInt, uuid: UUID)

object Other extends ShapeTag.Companion[Other] {
  val id: ShapeId = ShapeId("smithy4s.example", "Other")

  val hints: Hints = Hints(
    alloy.proto.ProtoEnabled(),
  ).lazily

  // constructor using the original order from the spec
  private def make(bigDecimal: BigDecimal, bigInteger: BigInt, uuid: UUID): Other = Other(bigDecimal, bigInteger, uuid)

  implicit val schema: Schema[Other] = struct(
    bigdecimal.required[Other]("bigDecimal", _.bigDecimal).addHints(alloy.proto.ProtoIndex(3)),
    bigint.required[Other]("bigInteger", _.bigInteger).addHints(alloy.proto.ProtoIndex(4)),
    uuid.required[Other]("uuid", _.uuid).addHints(alloy.proto.ProtoIndex(5)),
  )(make).withId(id).addHints(hints)
}
