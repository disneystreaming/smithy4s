package smithy4s

import munit._
import smithy4s.example.ConstrainedList
import smithy4s.example.ConstrainedMap

class ConstraintsSmokeSpec extends FunSuite {

  test("list members with length trait are rendered as length refinements") {
    matches(ConstrainedList.schema) {
      case Schema.BijectionSchema(
            Schema.CollectionSchema(
              _,
              _,
              _,
              refinement
            ),
            _
          ) =>
        isLengthRefinement(refinement)(min = 1, max = 11)
    }
  }

  test("map with length traits are rendered as length refinements") {
    matches(ConstrainedMap.schema) {
      case bijectionSchema: Schema.BijectionSchema[_, _] =>
        matches(bijectionSchema.underlying) {
          case Schema.MapSchema(
                _,
                _,
                keySchema,
                valueSchema
              ) =>
            isLengthRefinement(keySchema)(min = 2, max = 12)
            isLengthRefinement(valueSchema)(min = 3, max = 13)
        }
    }
  }

  private def isLengthRefinement(
      schema: Schema[_]
  )(min: Long, max: Long): Unit = matches(schema) {
    case Schema.RefinementSchema(_, refinement) =>
      matches(refinement.constraint) { case lengthRef: smithy.api.Length =>
        assertEquals(lengthRef.min, Some(min))
        assertEquals(lengthRef.max, Some(max))
      }
  }

  private def matches[A](value: A)(f: PartialFunction[A, Unit]): Unit = {
    if (f.isDefinedAt(value)) f(value)
    else fail(s"Value did not match: $value")
  }

}
