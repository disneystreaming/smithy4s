package smithy.test

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.map
import smithy4s.schema.Schema.string

object HttpMalformedRequestTestParametersDefinition extends Newtype[Map[String, List[String]]] {
  val id: ShapeId = ShapeId("smithy.test", "HttpMalformedRequestTestParametersDefinition")
  val hints: Hints = Hints(
    smithy.api.Private(),
  )
  val underlyingSchema: Schema[Map[String, List[String]]] = map(string, StringList.underlyingSchema).withId(id).addHints(hints)
  implicit val schema: Schema[HttpMalformedRequestTestParametersDefinition] = bijection(underlyingSchema, asBijection)
}
