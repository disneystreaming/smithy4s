package smithy4s

import weaver._

object EmptyServiceSmokeSpec extends FunSuite {

  test("Empty services do compile") {
    val version = smithy4s.example.EmptyService.version
    expect.eql(version, "1.0")
  }

}
