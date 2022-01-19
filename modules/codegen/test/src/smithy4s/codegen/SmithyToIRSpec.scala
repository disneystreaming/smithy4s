package smithy4s.codegen

import weaver._

object SmithyToIRSpec extends FunSuite {

  test("prettifyName: sdkId takes precedence") {
    expect.eql(
      SmithyToIR.prettifyName(Some("Example"), "unused"),
      "Example"
    )
  }
  test("prettifyName: shapeName is used as a fallback") {
    expect.eql(
      SmithyToIR.prettifyName(None, "Example"),
      "Example"
    )
  }

  test("prettifyName removes whitespace in sdkId") {
    expect.eql(
      SmithyToIR.prettifyName(Some("QuickDB \t\nStreams"), "unused"),
      "QuickDBStreams"
    )
  }

  // Not a feature, just verifying the name is unaffected
  test("prettifyName ignores whitespace in shape name") {
    expect.eql(
      SmithyToIR.prettifyName(None, "This Has Spaces"),
      "This Has Spaces"
    )
  }
}
