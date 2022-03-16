package smithy4s

import smithy4s.schema._
import smithy4s.schema.Schema._

// Verifies equality of implicit values
object SchemaEqualitySmokeSpec extends weaver.FunSuite {

  case class Recursive(foo: Option[Recursive])

  object Recursive {
    implicit val recSchema: Schema[Recursive] = recursive(
      struct(recSchema.optional[Recursive]("foo", _.foo))(Recursive.apply)
    )
  }

  test("Recursive") {
    val summoned1 = implicitly[Schema[Recursive]]
    val summoned2 = implicitly[Schema[Recursive]]

    expect(summoned1 == summoned2)
  }

}
