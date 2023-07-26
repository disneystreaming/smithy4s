package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union

sealed trait OpticsUnion extends scala.Product with scala.Serializable {
  @inline final def widen: OpticsUnion = this
  def _ordinal: Int
}
object OpticsUnion extends ShapeTag.Companion[OpticsUnion] {
  final case class OneCase(one: OpticsStructure) extends OpticsUnion { final def _ordinal: Int = 0 }
  def one(one:OpticsStructure): OpticsUnion = OneCase(one)

  object OneCase {
    val schema: Schema[OneCase] = bijection(OpticsStructure.schema
    .addHints(
      Hints.empty
    )
    , OneCase(_), _.one)
    val alt = schema.oneOf[OpticsUnion]("one")
  }

  implicit val schema: Schema[OpticsUnion] = union(
    OneCase.alt,
  ){
    _._ordinal
  }
  .withId(ShapeId("smithy4s.example", "OpticsUnion"))
  .addHints(
    Hints.empty
  )
}
