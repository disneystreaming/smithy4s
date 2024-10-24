package smithy4s.example.collision

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.union

sealed trait Class extends scala.Product with scala.Serializable { self =>
  @inline final def widen: Class = this
  def $ordinal: Int

  object project {
    def _package: Option[Class.AdtStruct] = Class.AdtStruct.alt.project.lift(self)
  }

  def accept[A](visitor: Class.Visitor[A]): A = this match {
    case value: Class.AdtStruct => visitor._package(value)
  }
}
object Class extends ShapeTag.Companion[Class] {

  def adtStruct():AdtStruct = AdtStruct()

  val id: ShapeId = ShapeId("smithy4s.example.collision", "class")

  val hints: Hints = Hints(
    smithy.api.Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
  ).lazily

  final case class AdtStruct() extends Class {
    def $ordinal: Int = 0
  }

  object AdtStruct {
    val id: ShapeId = ShapeId("smithy4s.example.collision", "AdtStruct")

    val hints: Hints = Hints.empty


    val schema: Schema[AdtStruct] = constant(AdtStruct()).withId(id).addHints(hints)

    val alt = schema.oneOf[Class]("package")
  }


  trait Visitor[A] {
    def _package(value: Class.AdtStruct): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def _package(value: Class.AdtStruct): A = default
    }
  }

  implicit val schema: Schema[Class] = recursive(union(
    Class.AdtStruct.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints))
}
