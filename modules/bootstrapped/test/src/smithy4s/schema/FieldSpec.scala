package smithy4s
package schema

import munit._
import Schema._
import smithy.api.Default
import scala.collection.mutable.ListBuffer

final class FieldSpec extends FunSuite {

  test("foreachUnlessDefault") {
    case class Foo(a: String)
    val field = string
      .required[Foo]("a", _.a)
      .addHints(Default(Document.fromString("test")))

    val result = ListBuffer.empty[String]
    field.foreachUnlessDefault(Foo("test")) { foo =>
      result += foo
    }

    field.foreachUnlessDefault(Foo("test2")) { foo =>
      result += foo
    }

    assertEquals(result.toList, List("test2"))
  }

}
