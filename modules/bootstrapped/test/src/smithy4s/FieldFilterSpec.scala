package smithy4s

import munit.FunSuite
import smithy4s.schema.FieldFilter
import smithy4s.schema.Field
import munit.Location

class FieldFilterSpec extends FunSuite {

  test("SkipUnsetOptions should remove Nones") {
    check(Schema.string.option, None, false)
  }

  test("SkipUnsetOptions should remove Nones bijected to something else") {
    case class OptionalLike[+A](underlying: Option[A])

    check(
      Schema.string.option.biject(OptionalLike(_))(_.underlying),
      OptionalLike(None),
      false
    )
  }

  test("SkipUnsetOptions should keep null nullables") {
    check(Schema.string.nullable, Nullable.Null, true)
  }

  test("SkipUnsetOptions should skip a None if it contains a nullable") {
    check(Schema.string.nullable.option, None, false)
  }

  test("SkipUnsetOptions should keep optional null nullables when present") {
    check(Schema.string.nullable.option, Some(Nullable.Null), true)
  }

  private def check[A](schema: Schema[A], value: A, expectedToKeep: Boolean)(
      implicit loc: Location
  ) = {
    val filter =
      FieldFilter.SkipUnsetOptions.compile(Field("label", schema, identity[A]))
    assertEquals(filter(value), expectedToKeep)
  }

}
