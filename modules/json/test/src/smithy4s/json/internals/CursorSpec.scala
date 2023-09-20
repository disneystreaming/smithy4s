package smithy4s.json.internals

import munit._
import smithy4s.json.internals.Cursor
import smithy4s.codecs.PayloadPath

final class CursorSpec extends FunSuite {

  test("check push / pop accuracy") {
    val c = new Cursor()
    c.push("test")
    c.pop()
    c.push(1)
    assertEquals(
      c.getPath(List.empty),
      PayloadPath(List(PayloadPath.Segment(1)))
    )
  }

}
