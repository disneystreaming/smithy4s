package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

/** This is meant to be used with `$`{ENV_VAR}
  * @param member
  *   This is meant to be used with `$`ENV_VAR
  */
final case class EnvVarString(member: Option[String] = None)

object EnvVarString extends ShapeTag.Companion[EnvVarString] {
  val id: ShapeId = ShapeId("smithy4s.example", "EnvVarString")

  val hints: Hints = Hints(
    smithy.api.Documentation(s"This is meant to be used with $${ENV_VAR}"),
  )

  implicit val schema: Schema[EnvVarString] = struct(
    string.optional[EnvVarString]("member", _.member).addHints(smithy.api.Documentation(s"This is meant to be used with $$ENV_VAR")),
  ){
    EnvVarString.apply
  }.withId(id).addHints(hints)
}
