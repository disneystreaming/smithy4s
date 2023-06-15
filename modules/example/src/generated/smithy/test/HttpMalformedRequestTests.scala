package smithy.test

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.recursive

/** Define how a malformed HTTP request is rejected by a server given a specific protocol */
object HttpMalformedRequestTests extends Newtype[List[HttpMalformedRequestTestCase]] {
  val id: ShapeId = ShapeId("smithy.test", "httpMalformedRequestTests")
  val hints: Hints = Hints(
    smithy.api.Documentation("Define how a malformed HTTP request is rejected by a server given a specific protocol"),
    smithy.api.Unstable(),
    smithy.api.Trait(selector = Some("operation"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )
  val underlyingSchema: Schema[List[HttpMalformedRequestTestCase]] = list(HttpMalformedRequestTestCase.schema).withId(id).addHints(hints).validated(smithy.api.Length(min = Some(1L), max = None))
  implicit val schema: Schema[HttpMalformedRequestTests] = recursive(bijection(underlyingSchema, asBijection))
}
