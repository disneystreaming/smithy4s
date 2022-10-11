package smithy4s

import munit._

class HintsEqualitySpec() extends FunSuite {

  test("the Hints construct has an implemented equality method") {
    def hints() = Hints(smithy.api.Deprecated())
    val hints1 = hints()
    val hints2 = hints()
    assertEquals(hints1, hints2)
  }

}
