package aws.protocols

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.set

object ChecksumAlgorithmSet extends Newtype[Set[ChecksumAlgorithm]] {
  val id: ShapeId = ShapeId("aws.protocols", "ChecksumAlgorithmSet")
  val hints: Hints = Hints(
    smithy.api.UniqueItems(),
    smithy.api.Private(),
  )
  val underlyingSchema: Schema[Set[ChecksumAlgorithm]] = set(ChecksumAlgorithm.schema).withId(id).addHints(hints).validated(smithy.api.Length(min = Some(1L), max = None))
  implicit val schema: Schema[ChecksumAlgorithmSet] = bijection(underlyingSchema, asBijection)
}
