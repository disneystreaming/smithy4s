package smithy4s.example

import smithy4s.Bijection
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

  object OneCase {
    implicit val fromValue: Bijection[OpticsStructure, OneCase] = Bijection(OneCase(_), _.one)
    implicit val toValue: Bijection[OneCase, OpticsStructure] = fromValue.swap
    val schema: Schema[OneCase] = bijection(OpticsStructure.schema, fromValue)
  }

  val one = OneCase.schema.oneOf[OpticsUnion]("one")

  implicit val schema: Schema[OpticsUnion] = union(
    one,
  ){
    _._ordinal
  }
  .withId(ShapeId("smithy4s.example", "OpticsUnion"))
}
