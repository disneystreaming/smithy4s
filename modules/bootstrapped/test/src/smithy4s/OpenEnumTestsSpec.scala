package smithy4s.example

import munit.FunSuite

class OpenEnumTestsSpec extends FunSuite {

  test("OpenEnumTest uses fromStringOrUnknown for known and unknown values") {
    assertEquals(OpenEnumTest.fromStringOrUnknown("ONE"), OpenEnumTest.ONE)
    assertEquals(
      OpenEnumTest.fromStringOrUnknown("UNKNOWN_VALUE"),
      OpenEnumTest.$Unknown("UNKNOWN_VALUE")
    )
  }

  test("OpenIntEnumTest uses fromIntOrUnknown for known and unknown values") {
    assertEquals(OpenIntEnumTest.fromIntOrUnknown(1), OpenIntEnumTest.ONE)
    assertEquals(
      OpenIntEnumTest.fromIntOrUnknown(999),
      OpenIntEnumTest.$Unknown(999)
    )
  }

}
