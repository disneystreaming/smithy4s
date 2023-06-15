package smithy.test

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.recursive

/** Define how an HTTP request is serialized given a specific protocol,
  * authentication scheme, and set of input parameters.
  */
object HttpRequestTests extends Newtype[List[HttpRequestTestCase]] {
  val id: ShapeId = ShapeId("smithy.test", "httpRequestTests")
  val hints: Hints = Hints(
    smithy.api.Documentation("Define how an HTTP request is serialized given a specific protocol,\nauthentication scheme, and set of input parameters."),
    smithy.api.Trait(selector = Some("operation"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )
  val underlyingSchema: Schema[List[HttpRequestTestCase]] = list(HttpRequestTestCase.schema).withId(id).addHints(hints).validated(smithy.api.Length(min = Some(1L), max = None))
  implicit val schema: Schema[HttpRequestTests] = recursive(bijection(underlyingSchema, asBijection))
}
