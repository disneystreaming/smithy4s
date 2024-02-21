package smithy4s
package schema

import munit._
import Schema._
import smithy.api.Default
import scala.collection.mutable.ListBuffer

final class FieldSpec extends FunSuite {

  test(
    "foreachUnlessDefault always applies the lambda when fields are required"
  ) {
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

    assertEquals(result.toList, List("test", "test2"))
  }

  test(
    "foreachUnlessDefault skips the lambda if an optional field has a default value"
  ) {
    case class Foo(a: Option[String])
    val field = string
      .optional[Foo]("a", _.a)
      .addHints(Default(Document.fromString("test")))

    val result = ListBuffer.empty[Option[String]]
    field.foreachUnlessDefault(Foo(Some("test"))) { foo =>
      result += foo
    }

    field.foreachUnlessDefault(Foo(Some("test2"))) { foo =>
      result += foo
    }

    field.foreachUnlessDefault(Foo(None)) { foo =>
      result += foo
    }

    assertEquals(result.toList, List(Some("test2"), None))
  }

  test(
    "getUnlessDefault can distinguish between the type's default value and null for nullable fields"
  ) {
    import Nullable._
    case class Foo(
        a: Nullable[String],
    )

    val emptyStringDefault = string.nullable
      .field[Foo]("a", _.a)
      .addHints(Default(Document.fromString("")))

    val nullDefault = string.nullable
      .field[Foo]("a", _.a)
      .addHints(Default(Document.DNull))

    assertEquals(emptyStringDefault.getUnlessDefault(Foo(Value(""))), None)
    assertEquals(emptyStringDefault.getUnlessDefault(Foo(Null)), Some(Null))
    assertEquals(nullDefault.getUnlessDefault(Foo(Value(""))), Some(Value("")))
    assertEquals(nullDefault.getUnlessDefault(Foo(Null)), None)

  }
}
