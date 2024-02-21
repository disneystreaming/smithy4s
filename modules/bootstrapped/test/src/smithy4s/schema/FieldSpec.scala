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
        a: Nullable[String]
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

  test(
    "getUnlessDefault can handle required nullable fields"
  ) {
    case class Foo(
        a: Nullable[String]
    )

    val requiredNoDefault = string.nullable
      .required[Foo]("a", _.a)

    val requiredDefaultNull = string.nullable
      .required[Foo]("a", _.a)
      .addHints(Default(Document.DNull))

    val requiredDefaultValue = string.nullable
      .required[Foo]("a", _.a)
      .addHints(Default(Document.fromString("default")))

    // required should always return no matter what
    List(Nullable.Value("default"), Nullable.Value(""), Nullable.Null).foreach {
      test =>
        assertEquals(requiredNoDefault.getUnlessDefault(Foo(test)), Some(test))
        assertEquals(
          requiredDefaultNull.getUnlessDefault(Foo(test)),
          Some(test)
        )
        assertEquals(
          requiredDefaultValue.getUnlessDefault(Foo(test)),
          Some(test)
        )
    }
  }

  test(
    "getUnlessDefault can handle optional fields, treating None as the default"
  ) {
    case class Foo(a: Option[String])

    val optional = string.optional[Foo]("a", _.a)

    expect.eql(optional.getUnlessDefault(Foo(Some(""))), Some(Some("")))
    expect.eql(optional.getUnlessDefault(Foo(None)), None)
  }

  test(
    "getUnlessDefault can handle optional nullable fields, treating None as the default"
  ) {
    case class Foo(
        a: Option[Nullable[String]]
    )

    val optional = string.nullable
      .optional[Foo]("a", _.a)

    List(
      Some(Nullable.Value("default")),
      Some(Nullable.Value("")),
      Some(Nullable.Null)
    ).foreach { test =>
      assertEquals(optional.getUnlessDefault(Foo(test)), Some(test))
    }
    assertEquals(optional.getUnlessDefault(Foo(None)), None)
  }
}
