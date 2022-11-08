package smithy4s
package schema

import Schema._

final class AltSpec extends munit.FunSuite {

  test("dispatcher projector") {
    type Foo = Either[Int, String]
    val left = int.oneOf[Foo]("left", Left(_))
    val right = string.oneOf[Foo]("right", Right(_))
    val schema = union(left, right) {
      case Left(int)     => left(int)
      case Right(string) => right(string)
    }

    val dispatcher = Alt.Dispatcher(schema.alternatives, schema.dispatch)

    val projectedLeft = dispatcher.projector(schema.alternatives.head)

    val projectedRight = dispatcher.projector(schema.alternatives.last)

    assertEquals(projectedLeft(Left(100)), Some(100))
    assertEquals(projectedLeft(Right("100")), None)
    assertEquals(projectedRight(Right("100")), Some("100"))
    assertEquals(projectedRight(Left(100)), None)
  }

}
