package smithy4s.codegen.internals

class NamespacePatternSpec extends munit.FunSuite {

  test("NamespacePattern.matches - positive examples") {
    List(
      "a.b" -> "a.b",
      "a.b.*" -> "a.b.c",
      "a.b.*" -> "a.b.c.d",
      "a.b*" -> "a.b",
      "a.b.*" -> "a.b.C",
      "a.b*" -> "a.bc",
      "a.b*" -> "a.bC",
      "a.b*" -> "a.b.c",
      "a.b*" -> "a.b.C"
    ).foreach { case (pattern, namespace) =>
      assert(
        NamespacePattern.fromString(pattern).matches(namespace),
        s"Pattern '$pattern' expected to match a namespace '$namespace'"
      )
    }
  }

  test("NamespacePattern.matches - negative examples") {
    List(
      "a.b" -> "a.c",
      "a.b" -> "a.B",
      "a.b" -> "acb",
      "a.b.*" -> "a.b",
      "a.b.*" -> "a.B",
      "a.b.*" -> "b.a",
      "a.b.*" -> "a.bb.c",
      "a.b.*" -> "acb.d",
      "a.b*" -> "acb",
      "a.b*" -> "b.a.c",
      "a.b.c-d" -> "a.b.c-d"
    ).foreach { case (pattern, namespace) =>
      assert(
        !NamespacePattern.fromString(pattern).matches(namespace),
        s"Pattern '$pattern' not expected to match a namespace '$namespace'"
      )
    }
  }
}
