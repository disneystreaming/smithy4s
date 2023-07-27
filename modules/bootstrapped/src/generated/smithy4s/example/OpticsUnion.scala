package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Prism
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union

sealed trait OpticsUnion extends scala.Product with scala.Serializable {
  @inline final def widen: OpticsUnion = this
  def _ordinal: Int
}
object OpticsUnion extends ShapeTag.Companion[OpticsUnion] {
  val id: ShapeId = ShapeId("smithy4s.example", "OpticsUnion")

  val hints: Hints = Hints.empty

  object optics {
    val one: Prism[OpticsUnion, OpticsStructure] = Prism.partial[OpticsUnion, OpticsStructure]{ case OneCase(t) => t }(OneCase.apply)
  }

  final case class OneCase(one: OpticsStructure) extends OpticsUnion { final def _ordinal: Int = 0 }
  def one(one:OpticsStructure): OpticsUnion = OneCase(one)

  object OneCase {
    val hints: Hints = Hints.empty
    val schema: Schema[OneCase] = bijection(OpticsStructure.schema.addHints(hints), OneCase(_), _.one)
    val alt = schema.oneOf[OpticsUnion]("one")
  }

  implicit val schema: Schema[OpticsUnion] = union(
    OneCase.alt,
  ){
    _._ordinal
  }.withId(id).addHints(hints)
}
