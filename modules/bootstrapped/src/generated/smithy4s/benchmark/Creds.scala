package smithy4s.benchmark

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Creds(user: Option[String] = None, key: Option[String] = None)
object Creds extends ShapeTag.$Companion[Creds] {
  val $id: ShapeId = ShapeId("smithy4s.benchmark", "Creds")

  val $hints: Hints = Hints.empty

  val user: FieldLens[Creds, Option[String]] = string.optional[Creds]("user", _.user, n => c => c.copy(user = n))
  val key: FieldLens[Creds, Option[String]] = string.optional[Creds]("key", _.key, n => c => c.copy(key = n))

  implicit val $schema: Schema[Creds] = struct(
    user,
    key,
  ){
    Creds.apply
  }.withId($id).addHints($hints)
}
