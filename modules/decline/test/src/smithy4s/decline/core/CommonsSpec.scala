package smithy4s.decline.core

import weaver._

object CommonsSpec extends FunSuite {

  test("toKebabCase base case") {
    expect(
      commons.toKebabCase("StartsWithUpperCase") == "starts-with-upper-case"
    )
  }

  test("toKebabCase starts with lower case") {
    expect(
      commons.toKebabCase("startsWithLowerCase") == "starts-with-lower-case"
    )
  }
}
