package smithy4s.http.internals

import smithy4s.http.PathSegment

object PathParsingSpec extends weaver.FunSuite {

  test("Parse path pattern into path segments") {
    val result = pathSegments("/{head}/foo/{tail+}")
    expect(
      result == Option(
        Vector(
          PathSegment.label("head"),
          PathSegment.static("foo"),
          PathSegment.greedy("tail")
        )
      )
    )
  }

}
