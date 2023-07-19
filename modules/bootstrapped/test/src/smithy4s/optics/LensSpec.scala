package smithy4s.optics

import munit._
import smithy4s.example.EchoBody

// inspired by and adapted from https://www.optics.dev/Monocle/ under the MIT license
final class LensSpec extends FunSuite {

  test("get and replace") {
    val lens = EchoBody.Optics.data
    val e = EchoBody(Some("test body"))
    val result = lens.replace(lens.get(e))(e)
    assertEquals(e, result)
  }

  test("replace and get") {
    val lens = EchoBody.Optics.data
    val e = EchoBody(Some("test body"))
    val result = lens.get(lens.replace(Some("test body"))(e))
    assertEquals(e.data, result)
  }

  test("replace idempotent") {
    val lens = EchoBody.Optics.data
    val data = Some("test body")
    val e = EchoBody(data)
    val result = lens.replace(data)(lens.replace(data)(e))
    assertEquals(e, result)
  }

  test("modify identity") {
    val lens = EchoBody.Optics.data
    val data = Some("test body")
    val e = EchoBody(data)
    val result = lens.modify(identity)(e)
    assertEquals(e, result)
  }

  test("modify composition") {
    val lens = EchoBody.Optics.data
    val data = Some("test body")
    val e = EchoBody(data)
    val f: Option[String] => Option[String] = _ => Some("test 2")
    val g: Option[String] => Option[String] = _ => Some("test 3")
    val resultOne = lens.modify(g)(lens.modify(f)(e))
    val resultTwo = lens.modify(g compose f)(e)
    assertEquals(EchoBody(Some("test 3")), resultOne)
    assertEquals(resultOne, resultTwo)
  }

  test("modify == replace") {
    val lens = EchoBody.Optics.data
    val data = Some("test body")
    val data2 = Some("test body 2")
    val e = EchoBody(data)
    val resultOne = lens.replace(data2)(e)
    val resultTwo = lens.modify(_ => data2)(e)
    assertEquals(EchoBody(Some("test body 2")), resultOne)
    assertEquals(resultOne, resultTwo)
  }

  case class Point(x: Int, y: Int)
  case class Example(s: String, p: Point)
  val s = Lens[Example, String](_.s)(s => ex => ex.copy(s = s))
  val p = Lens[Example, Point](_.p)(p => ex => ex.copy(p = p))

  val x = Lens[Point, Int](_.x)(x => p => p.copy(x = x))
  val y = Lens[Point, Int](_.y)(y => p => p.copy(y = y))
  val xy = Lens[Point, (Int, Int)](p => (p.x, p.y))(xy =>
    p => p.copy(x = xy._1, y = xy._2)
  )

  test("get") {
    assertEquals(x.get(Point(5, 2)), 5)
  }

  test("set") {
    assertEquals(x.replace(5)(Point(9, 2)), Point(5, 2))
  }

  test("modify") {
    assertEquals(x.modify(_ + 1)(Point(9, 2)), Point(10, 2))
  }

  test("some") {
    case class SomeTest(x: Int, y: Option[Int])
    val obj = SomeTest(1, Some(2))

    val lens = Lens((_: SomeTest).y)(newValue => _.copy(y = newValue))

    assertEquals(lens.some.getOption(obj), Some(2))
  }

}
