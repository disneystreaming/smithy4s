package smithy4s.codegen.internals

import cats.syntax.all._
import munit._

final class VersionNumberSpec extends FunSuite {
  test("VersionNumber ordering") {
    assert(VersionNumber.parse("1.0") < VersionNumber.parse("1.0.1"))
    assert(VersionNumber.parse("1.0.1") < VersionNumber.parse("1.1"))
    assert(VersionNumber.parse("1.1.2") < VersionNumber.parse("1.1.3"))
    assert(VersionNumber.parse("1.1.9") < VersionNumber.parse("1.1.11"))
  }
}
