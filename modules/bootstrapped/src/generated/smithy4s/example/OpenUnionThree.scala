package smithy4s.example

import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.document
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.union

sealed trait OpenUnionThree extends scala.Product with scala.Serializable { self =>
  @inline final def widen: OpenUnionThree = this
  def $ordinal: Int

  object project {
    def a: Option[String] = OpenUnionThree.ACase.alt.project.lift(self).map(_.a)
    def b: Option[Int] = OpenUnionThree.BCase.alt.project.lift(self).map(_.b)
    def z: Option[Boolean] = OpenUnionThree.ZCase.alt.project.lift(self).map(_.z)
    def unknown: Option[Document] = OpenUnionThree.UnknownCase.alt.project.lift(self).map(_.unknown)
  }

  def accept[A](visitor: OpenUnionThree.Visitor[A]): A = this match {
    case value: OpenUnionThree.ACase => visitor.a(value.a)
    case value: OpenUnionThree.BCase => visitor.b(value.b)
    case value: OpenUnionThree.ZCase => visitor.z(value.z)
    case value: OpenUnionThree.UnknownCase => visitor.unknown(value.unknown)
  }
}
object OpenUnionThree extends ShapeTag.Companion[OpenUnionThree] {

  def a(a: String): OpenUnionThree = ACase(a)
  def b(b: Int): OpenUnionThree = BCase(b)
  def z(z: Boolean): OpenUnionThree = ZCase(z)
  def unknown(unknown: Document): OpenUnionThree = UnknownCase(unknown)

  val id: ShapeId = ShapeId("smithy4s.example", "OpenUnionThree")

  val hints: Hints = Hints.empty

  final case class ACase(a: String) extends OpenUnionThree { final def $ordinal: Int = 0 }
  final case class BCase(b: Int) extends OpenUnionThree { final def $ordinal: Int = 1 }
  final case class ZCase(z: Boolean) extends OpenUnionThree { final def $ordinal: Int = 2 }
  final case class UnknownCase(unknown: Document) extends OpenUnionThree { final def $ordinal: Int = 3 }

  object ACase {
    val hints: Hints = Hints.empty
    val schema: Schema[OpenUnionThree.ACase] = bijection(string.addHints(hints), OpenUnionThree.ACase(_), _.a)
    val alt = schema.oneOf[OpenUnionThree]("a")
  }
  object BCase {
    val hints: Hints = Hints.empty
    val schema: Schema[OpenUnionThree.BCase] = bijection(int.addHints(hints), OpenUnionThree.BCase(_), _.b)
    val alt = schema.oneOf[OpenUnionThree]("b")
  }
  object ZCase {
    val hints: Hints = Hints.empty
    val schema: Schema[OpenUnionThree.ZCase] = bijection(boolean.addHints(hints), OpenUnionThree.ZCase(_), _.z)
    val alt = schema.oneOf[OpenUnionThree]("z")
  }
  object UnknownCase {
    val hints: Hints = Hints(
      alloy.JsonUnknown(),
    ).lazily
    val schema: Schema[OpenUnionThree.UnknownCase] = bijection(document.addHints(hints), OpenUnionThree.UnknownCase(_), _.unknown)
    val alt = schema.oneOf[OpenUnionThree]("unknown")
  }

  trait Visitor[A] {
    def a(value: String): A
    def b(value: Int): A
    def z(value: Boolean): A
    def unknown(value: Document): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def a(value: String): A = default
      def b(value: Int): A = default
      def z(value: Boolean): A = default
      def unknown(value: Document): A = default
    }
  }

  implicit val schema: Schema[OpenUnionThree] = union(
    OpenUnionThree.ACase.alt,
    OpenUnionThree.BCase.alt,
    OpenUnionThree.ZCase.alt,
    OpenUnionThree.UnknownCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
