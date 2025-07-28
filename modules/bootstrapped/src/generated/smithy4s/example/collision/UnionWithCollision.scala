package smithy4s.example.collision

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.union

sealed trait UnionWithCollision extends scala.Product with scala.Serializable { self =>
  @inline final def widen: UnionWithCollision = this
  def $ordinal: Int

  object project {
    def s: Option[UnionWithCollision.Struct] = UnionWithCollision.Struct.alt.project.lift(self)
  }

  def accept[A](visitor: UnionWithCollision.Visitor[A]): A = this match {
    case value: UnionWithCollision.Struct => visitor.s(value)
  }
}
object UnionWithCollision extends ShapeTag.Companion[UnionWithCollision] {

  def struct(name: Option[String] = None): Struct = Struct(name)

  val id: ShapeId = ShapeId("smithy4s.example.collision", "UnionWithCollision")

  val hints: Hints = Hints.empty

  final case class Struct(name: Option[String] = None) extends UnionWithCollision {
    def $ordinal: Int = 0
  }

  object Struct {
    val id: ShapeId = ShapeId("smithy4s.example.collision", "Struct")

    val hints: Hints = Hints.empty

    // constructor using the original order from the spec
    private def make(name: Option[String]): Struct = Struct(name)

    val schema: Schema[Struct] = smithy4s.schema.Schema.struct(
      String.schema.optional[Struct]("name", _.name),
    )(make).withId(id).addHints(hints)

    val alt = schema.oneOf[UnionWithCollision]("s")
  }


  trait Visitor[A] {
    def s(value: UnionWithCollision.Struct): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def s(value: UnionWithCollision.Struct): A = default
    }
  }

  implicit val schema: Schema[UnionWithCollision] = union(
    UnionWithCollision.Struct.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
