package smithy4s.optics

import munit._
import smithy4s.example.Podcast

final class PrismSpec extends FunSuite {

  test("round trip") {
    val prism = Podcast.Optics.video
    val v = Podcast.Video(Some("My Title"))
    val result =
      prism.getOption(prism.project(v))
    assertEquals(Option(v), result)
  }

  test("round trip - empty") {
    val prism = Podcast.Optics.audio
    val v: Podcast = Podcast.Video(Some("My Title"))
    val result =
      prism.getOption(v).map(prism.project)
    assertEquals(Option.empty[Podcast], result)
  }

  test("modify identity") {
    val prism = Podcast.Optics.video
    val v = Podcast.Video(Some("My Title"))
    val result =
      prism.modify(identity)(v)
    assertEquals(v.widen, result)
  }

  test("modify compose") {
    val prism = Podcast.Optics.video
    val v = Podcast.Video(Some("My Title"))
    val f: Podcast.Video => Podcast.Video = _.copy(title = Some("Title 2"))
    val g: Podcast.Video => Podcast.Video = _.copy(title = Some("Title 3"))
    val resultOne =
      prism.modify(g)(prism.modify(f)(v))
    val resultTwo = prism.modify(g compose f)(v)
    assertEquals(Podcast.Video(Some("Title 3")).widen, resultOne)
    assertEquals(resultTwo, resultOne)
  }

  test("modify == replace") {
    val prism = Podcast.Optics.video
    val v = Podcast.Video(Some("My Title"))
    val v2 = Podcast.Video(Some("My Title 2"))
    val resultOne =
      prism.modify(_ => v2)(v)
    val resultTwo = prism.replace(v2)(v)
    assertEquals(Podcast.Video(Some("My Title 2")).widen, resultOne)
    assertEquals(resultTwo, resultOne)
  }

  sealed trait IntOrString
  case class I(i: Int) extends IntOrString
  case class S(s: String) extends IntOrString

  val i = Prism
    .partial[IntOrString, I] { case i: I => i }(identity)
  val s =
    Prism[IntOrString, String] { case S(s) => Some(s); case _ => None }(S.apply)

  test("getOption") {
    assertEquals(i.getOption(I(1)), Option(I(1)))
    assertEquals(i.getOption(S("")), None)

    assertEquals(s.getOption(S("hello")), Some("hello"))
    assertEquals(s.getOption(I(10)), None)
  }

  test("project") {
    assertEquals(i.project(I(3)), I(3))
    assertEquals(s.project("Yop"), S("Yop"))
  }

  test("some") {
    case class SomeTest(y: Option[Int])
    val obj = SomeTest(Some(2))

    val prism =
      Prism[SomeTest, Option[Int]](i => Some(i.y))(SomeTest.apply)

    assertEquals(prism.some.getOption(obj), Some(2))
  }

}
