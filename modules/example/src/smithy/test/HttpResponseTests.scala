package smithy.test

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.recursive

/** Define how an HTTP response is serialized given a specific protocol,
  * authentication scheme, and set of output or error parameters.
  */
object HttpResponseTests extends Newtype[List[HttpResponseTestCase]] {
  val id: ShapeId = ShapeId("smithy.test", "httpResponseTests")
  val hints: Hints = Hints(
    smithy.api.Documentation("Define how an HTTP response is serialized given a specific protocol,\nauthentication scheme, and set of output or error parameters."),
    smithy.api.Trait(selector = Some(":test(operation, structure[trait|error])"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )
  val underlyingSchema: Schema[List[HttpResponseTestCase]] = list(HttpResponseTestCase.schema).withId(id).addHints(hints).validated(smithy.api.Length(min = Some(1L), max = None))
  implicit val schema: Schema[HttpResponseTests] = recursive(bijection(underlyingSchema, asBijection))
}
