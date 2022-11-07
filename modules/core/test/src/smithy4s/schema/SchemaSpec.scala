package smithy4s
package schema

import munit._
import Schema._
import smithy.api.Default

final class SchemaSpec extends FunSuite {

  test("getDefault - some") {
    val sch = string.addHints(Default(Document.fromString("test")))

    assertEquals(sch.getDefault, Some(Document.fromString("test")))
    assertEquals(sch.getDefaultValue, Some("test"))
  }

  test("getDefault - none") {
    val sch = string

    assertEquals(sch.getDefault, None)
    assertEquals(sch.getDefaultValue, None)
  }

}
