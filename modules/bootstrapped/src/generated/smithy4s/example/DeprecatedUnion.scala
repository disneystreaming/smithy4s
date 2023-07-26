package smithy4s.example

import smithy.api.Deprecated
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
object DeprecatedUnion extends ShapeTag.Companion[DeprecatedUnion] {
  @deprecated(message = "N/A", since = "N/A")
  final case class SCase(s: String) extends DeprecatedUnion { final def _ordinal: Int = 0 }
  def s(s:String): DeprecatedUnion = SCase(s)
  final case class S_V2Case(s_V2: String) extends DeprecatedUnion { final def _ordinal: Int = 1 }
  def s_V2(s_V2:String): DeprecatedUnion = S_V2Case(s_V2)
  @deprecated(message = "N/A", since = "N/A")
  final case class DeprecatedUnionProductCase() extends DeprecatedUnion {
    def _ordinal: Int = 2
  }
  object DeprecatedUnionProductCase extends ShapeTag.Companion[DeprecatedUnionProductCase] {

    implicit val schema: Schema[DeprecatedUnionProductCase] = constant(DeprecatedUnionProductCase()).withId(ShapeId("smithy4s.example", "DeprecatedUnionProductCase"))
    .withId(ShapeId("smithy4s.example", "DeprecatedUnionProductCase"))
    .addHints(
      Hints(
        Deprecated(message = None, since = None),
      )
    )

    val alt = schema.oneOf[DeprecatedUnion]("p")
  }
  @deprecated(message = "N/A", since = "N/A")
  final case class UnionProductCaseDeprecatedAtCallSite() extends DeprecatedUnion {
    def _ordinal: Int = 3
  }
  object UnionProductCaseDeprecatedAtCallSite extends ShapeTag.Companion[UnionProductCaseDeprecatedAtCallSite] {

    implicit val schema: Schema[UnionProductCaseDeprecatedAtCallSite] = constant(UnionProductCaseDeprecatedAtCallSite()).withId(ShapeId("smithy4s.example", "UnionProductCaseDeprecatedAtCallSite"))
    .withId(ShapeId("smithy4s.example", "UnionProductCaseDeprecatedAtCallSite"))
    .addHints(
      Hints(
        Deprecated(message = None, since = None),
      )
    )

    val alt = schema.oneOf[DeprecatedUnion]("p2")
  }

  object SCase {
    val schema: Schema[SCase] = bijection(string
    .addHints(
      Hints(
        Deprecated(message = None, since = None),
      )
    )
    , SCase(_), _.s)
    val alt = schema.oneOf[DeprecatedUnion]("s")
  }
  object S_V2Case {
    val schema: Schema[S_V2Case] = bijection(string
    .addHints(
      Hints.empty
    )
    , S_V2Case(_), _.s_V2)
    val alt = schema.oneOf[DeprecatedUnion]("s_V2")
  }

  implicit val schema: Schema[DeprecatedUnion] = union(
    SCase.alt,
    S_V2Case.alt,
    DeprecatedUnionProductCase.alt,
    UnionProductCaseDeprecatedAtCallSite.alt,
  ){
    _._ordinal
  }
  .withId(ShapeId("smithy4s.example", "DeprecatedUnion"))
  .addHints(
    Hints(
      Deprecated(message = Some("A compelling reason"), since = Some("0.0.1")),
    )
  )
}
