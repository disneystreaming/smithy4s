package smithy4s.example

import smithy4s.Hints
import smithy4s.Nullable
import smithy4s.Nullable.Null
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class PatchableEdgeCases(required: Nullable[Int], requiredDefaultValue: Nullable[Int] = smithy4s.Nullable.Value(3), requiredDefaultNull: Nullable[Int] = Null, defaultValue: Nullable[Int] = smithy4s.Nullable.Value(5), defaultNull: Nullable[Int] = Null)

object PatchableEdgeCases extends ShapeTag.Companion[PatchableEdgeCases] {
  val id: ShapeId = ShapeId("smithy4s.example", "PatchableEdgeCases")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[PatchableEdgeCases] = struct(
    int.nullable.required[PatchableEdgeCases]("required", _.required),
    int.nullable.required[PatchableEdgeCases]("requiredDefaultValue", _.requiredDefaultValue).addHints(smithy.api.Default(smithy4s.Document.fromDouble(3.0d))),
    int.nullable.required[PatchableEdgeCases]("requiredDefaultNull", _.requiredDefaultNull).addHints(smithy.api.Box(), smithy.api.Default(smithy4s.Document.nullDoc)),
    int.nullable.field[PatchableEdgeCases]("defaultValue", _.defaultValue).addHints(smithy.api.Default(smithy4s.Document.fromDouble(5.0d))),
    int.nullable.field[PatchableEdgeCases]("defaultNull", _.defaultNull).addHints(smithy.api.Box(), smithy.api.Default(smithy4s.Document.nullDoc)),
  ){
    PatchableEdgeCases.apply
  }.withId(id).addHints(hints)
}
