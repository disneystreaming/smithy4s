package smithy4s.example

import smithy4s.Bijection
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ValidatedNewtype
import smithy4s.Validator
import smithy4s.refined.NonEmptyList
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.string

object ValidatedRefinedList extends ValidatedNewtype[NonEmptyList[String]] {
  val id: ShapeId = ShapeId("smithy4s.example", "ValidatedRefinedList")
  val hints: Hints = Hints(
    smithy4s.example.NonEmptyListFormat(),
  ).lazily
  val underlyingSchema: Schema[NonEmptyList[String]] = list(string).refined[NonEmptyList[String]](smithy4s.example.NonEmptyListFormat()).withId(id).addHints(hints)
  val validator: Validator[NonEmptyList[String], ValidatedRefinedList] = Validator.Builder.list[String].refined[NonEmptyList[String]](smithy4s.example.NonEmptyListFormat()).biject(Bijection[NonEmptyList[String], ValidatedRefinedList](_.asInstanceOf[ValidatedRefinedList], value(_))).build()
  implicit val schema: Schema[ValidatedRefinedList] = validator.toSchema(underlyingSchema)
  @inline def apply(a: NonEmptyList[String]): Either[String, ValidatedRefinedList] = validator.validate(a)
}
