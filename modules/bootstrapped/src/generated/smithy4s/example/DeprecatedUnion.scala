package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.constant
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.union

@deprecated(message = "A compelling reason", since = "0.0.1")
sealed trait DeprecatedUnion extends scala.Product with scala.Serializable { self =>
  @inline final def widen: DeprecatedUnion = this
  def $ordinal: Int

  object project {
    def s: Option[String] = DeprecatedUnion.SCase.alt.project.lift(self).map(_.s)
    def s_V2: Option[String] = DeprecatedUnion.S_V2Case.alt.project.lift(self).map(_.s_V2)
    def p: Option[DeprecatedUnion.DeprecatedUnionProductCase] = DeprecatedUnion.DeprecatedUnionProductCase.alt.project.lift(self)
    def p2: Option[DeprecatedUnion.UnionProductCaseDeprecatedAtCallSite] = DeprecatedUnion.UnionProductCaseDeprecatedAtCallSite.alt.project.lift(self)
  }

  def accept[A](visitor: DeprecatedUnion.Visitor[A]): A = this match {
    case value: DeprecatedUnion.SCase => visitor.s(value.s)
    case value: DeprecatedUnion.S_V2Case => visitor.s_V2(value.s_V2)
    case value: DeprecatedUnion.DeprecatedUnionProductCase => visitor.p(value)
    case value: DeprecatedUnion.UnionProductCaseDeprecatedAtCallSite => visitor.p2(value)
  }
}
object DeprecatedUnion extends ShapeTag.Companion[DeprecatedUnion] {

  @deprecated(message = "N/A", since = "N/A")
  def s(s: String): DeprecatedUnion = SCase(s)
  def s_V2(s_V2: String): DeprecatedUnion = S_V2Case(s_V2)
  def deprecatedUnionProductCase():DeprecatedUnionProductCase = DeprecatedUnionProductCase()
  @deprecated(message = "N/A", since = "N/A")
  def unionProductCaseDeprecatedAtCallSite():UnionProductCaseDeprecatedAtCallSite = UnionProductCaseDeprecatedAtCallSite()

  val id: ShapeId = ShapeId("smithy4s.example", "DeprecatedUnion")

  val hints: Hints = Hints(
    smithy.api.Deprecated(message = Some("A compelling reason"), since = Some("0.0.1")),
  )

  @deprecated(message = "N/A", since = "N/A")
  final case class SCase(s: String) extends DeprecatedUnion { final def $ordinal: Int = 0 }
  final case class S_V2Case(s_V2: String) extends DeprecatedUnion { final def $ordinal: Int = 1 }
  @deprecated(message = "N/A", since = "N/A")
  final case class DeprecatedUnionProductCase() extends DeprecatedUnion {
    def $ordinal: Int = 2
  }

  object DeprecatedUnionProductCase extends ShapeTag.Companion[DeprecatedUnionProductCase] {
    val id: ShapeId = ShapeId("smithy4s.example", "DeprecatedUnionProductCase")

    val hints: Hints = Hints(
      smithy.api.Deprecated(message = None, since = None),
    )

    implicit val schema: Schema[DeprecatedUnionProductCase] = constant(DeprecatedUnionProductCase()).withId(id).addHints(hints)

    val alt = schema.oneOf[DeprecatedUnion]("p")
  }
  @deprecated(message = "N/A", since = "N/A")
  final case class UnionProductCaseDeprecatedAtCallSite() extends DeprecatedUnion {
    def $ordinal: Int = 3
  }

  object UnionProductCaseDeprecatedAtCallSite extends ShapeTag.Companion[UnionProductCaseDeprecatedAtCallSite] {
    val id: ShapeId = ShapeId("smithy4s.example", "UnionProductCaseDeprecatedAtCallSite")

    val hints: Hints = Hints(
      smithy.api.Deprecated(message = None, since = None),
    )

    implicit val schema: Schema[UnionProductCaseDeprecatedAtCallSite] = constant(UnionProductCaseDeprecatedAtCallSite()).withId(id).addHints(hints)

    val alt = schema.oneOf[DeprecatedUnion]("p2")
  }

  object SCase {
    val hints: Hints = Hints(
      smithy.api.Deprecated(message = None, since = None),
    )
    val schema: Schema[DeprecatedUnion.SCase] = bijection(string.addHints(hints), DeprecatedUnion.SCase(_), _.s)
    val alt = schema.oneOf[DeprecatedUnion]("s")
  }
  object S_V2Case {
    val hints: Hints = Hints.empty
    val schema: Schema[DeprecatedUnion.S_V2Case] = bijection(string.addHints(hints), DeprecatedUnion.S_V2Case(_), _.s_V2)
    val alt = schema.oneOf[DeprecatedUnion]("s_V2")
  }

  trait Visitor[A] {
    def s(value: String): A
    def s_V2(value: String): A
    def p(value: DeprecatedUnion.DeprecatedUnionProductCase): A
    def p2(value: DeprecatedUnion.UnionProductCaseDeprecatedAtCallSite): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def s(value: String): A = default
      def s_V2(value: String): A = default
      def p(value: DeprecatedUnion.DeprecatedUnionProductCase): A = default
      def p2(value: DeprecatedUnion.UnionProductCaseDeprecatedAtCallSite): A = default
    }
  }

  implicit val schema: Schema[DeprecatedUnion] = union(
    DeprecatedUnion.SCase.alt,
    DeprecatedUnion.S_V2Case.alt,
    DeprecatedUnion.DeprecatedUnionProductCase.alt,
    DeprecatedUnion.UnionProductCaseDeprecatedAtCallSite.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
