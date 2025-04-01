package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.union

sealed trait Items extends HasStuff with scala.Product with scala.Serializable { self =>
  @inline final def widen: Items = this
  def $ordinal: Int

  object project {
    def s1: Option[Items.Item1] = Items.Item1.alt.project.lift(self)
    def s2: Option[Items.Item2] = Items.Item2.alt.project.lift(self)
  }

  def accept[A](visitor: Items.Visitor[A]): A = this match {
    case value: Items.Item1 => visitor.s1(value)
    case value: Items.Item2 => visitor.s2(value)
  }
}
object Items extends ShapeTag.Companion[Items] {

  def item1(stuff: Option[String] = None):Item1 = Item1(stuff)
  def item2(stuff: Option[String] = None):Item2 = Item2(stuff)

  val id: ShapeId = ShapeId("smithy4s.example", "Items")

  val hints: Hints = Hints.empty

  final case class Item1(stuff: Option[String] = None) extends Items with HasHasStuff {
    def $ordinal: Int = 0
  }

  object Item1 {
    val id: ShapeId = ShapeId("smithy4s.example", "Item1")

    val hints: Hints = Hints.empty

    // constructor using the original order from the spec
    private def make(stuff: Option[String]): Item1 = Item1(stuff)

    val schema: Schema[Item1] = struct(
      string.optional[Item1]("stuff", _.stuff),
    )(make).withId(id).addHints(hints)

    val alt = schema.oneOf[Items]("s1")
  }
  final case class Item2(stuff: Option[String] = None) extends Items {
    def $ordinal: Int = 1
  }

  object Item2 {
    val id: ShapeId = ShapeId("smithy4s.example", "Item2")

    val hints: Hints = Hints.empty

    // constructor using the original order from the spec
    private def make(stuff: Option[String]): Item2 = Item2(stuff)

    val schema: Schema[Item2] = struct(
      string.optional[Item2]("stuff", _.stuff),
    )(make).withId(id).addHints(hints)

    val alt = schema.oneOf[Items]("s2")
  }


  trait Visitor[A] {
    def s1(value: Items.Item1): A
    def s2(value: Items.Item2): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def s1(value: Items.Item1): A = default
      def s2(value: Items.Item2): A = default
    }
  }

  implicit val schema: Schema[Items] = union(
    Items.Item1.alt,
    Items.Item2.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
