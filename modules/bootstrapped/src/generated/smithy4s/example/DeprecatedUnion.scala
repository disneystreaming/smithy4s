package smithy4s.example

import smithy.api.Deprecated
import smithy4s.Bijection
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.constant
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.union

@deprecated(message = "A compelling reason", since = "0.0.1")
sealed trait DeprecatedUnion extends scala.Product with scala.Serializable {
  @inline final def widen: DeprecatedUnion = this
  def _ordinal: Int
}
object DeprecatedUnion extends ShapeTag.$Companion[DeprecatedUnion] {

  @deprecated(message = "N/A", since = "N/A")
  def s(s:String): DeprecatedUnion = SCase(s)
  def s_V2(s_V2:String): DeprecatedUnion = S_V2Case(s_V2)
  def deprecatedUnionProductCase(): DeprecatedUnion = DeprecatedUnionProductCase()
  @deprecated(message = "N/A", since = "N/A")
  def unionProductCaseDeprecatedAtCallSite(): DeprecatedUnion = UnionProductCaseDeprecatedAtCallSite()

  val $id: ShapeId = ShapeId("smithy4s.example", "DeprecatedUnion")

  val $hints: Hints = Hints(
    Deprecated(message = Some("A compelling reason"), since = Some("0.0.1")),
  )

  @deprecated(message = "N/A", since = "N/A")
  final case class SCase(s: String) extends DeprecatedUnion { final def _ordinal: Int = 0 }
  final case class S_V2Case(s_V2: String) extends DeprecatedUnion { final def _ordinal: Int = 1 }
  @deprecated(message = "N/A", since = "N/A")
  final case class DeprecatedUnionProductCase() extends DeprecatedUnion {
    def _ordinal: Int = 2
  }
  object DeprecatedUnionProductCase extends ShapeTag.$Companion[DeprecatedUnionProductCase] {
    val $id: ShapeId = ShapeId("smithy4s.example", "DeprecatedUnionProductCase")

    val $hints: Hints = Hints(
      Deprecated(message = None, since = None),
    )

    implicit val $schema: Schema[DeprecatedUnionProductCase] = constant(DeprecatedUnionProductCase()).withId($id).addHints($hints)
  }
  @deprecated(message = "N/A", since = "N/A")
  final case class UnionProductCaseDeprecatedAtCallSite() extends DeprecatedUnion {
    def _ordinal: Int = 3
  }
  object UnionProductCaseDeprecatedAtCallSite extends ShapeTag.$Companion[UnionProductCaseDeprecatedAtCallSite] {
    val $id: ShapeId = ShapeId("smithy4s.example", "UnionProductCaseDeprecatedAtCallSite")

    val $hints: Hints = Hints(
      Deprecated(message = None, since = None),
    )

    implicit val $schema: Schema[UnionProductCaseDeprecatedAtCallSite] = constant(UnionProductCaseDeprecatedAtCallSite()).withId($id).addHints($hints)
  }

  object SCase {
    implicit val fromValue: Bijection[String, SCase] = Bijection(SCase(_), _.s)
    implicit val toValue: Bijection[SCase, String] = fromValue.swap
    val $schema: Schema[SCase] = bijection(string, fromValue).addHints(Deprecated(message = None, since = None))
  }
  object S_V2Case {
    implicit val fromValue: Bijection[String, S_V2Case] = Bijection(S_V2Case(_), _.s_V2)
    implicit val toValue: Bijection[S_V2Case, String] = fromValue.swap
    val $schema: Schema[S_V2Case] = bijection(string, fromValue)
  }

  val s = SCase.$schema.oneOf[DeprecatedUnion]("s")
  val s_V2 = S_V2Case.$schema.oneOf[DeprecatedUnion]("s_V2")
  val p = DeprecatedUnionProductCase.$schema.oneOf[DeprecatedUnion]("p")
  val p2 = UnionProductCaseDeprecatedAtCallSite.$schema.oneOf[DeprecatedUnion]("p2")

  implicit val $schema: Schema[DeprecatedUnion] = union(
    s,
    s_V2,
    p,
    p2,
  ){
    _._ordinal
  }.withId($id).addHints($hints)
}
