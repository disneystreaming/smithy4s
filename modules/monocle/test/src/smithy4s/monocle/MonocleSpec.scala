package smithy4s.monocle

import weaver._
import MonocleConversions._
import smithy4s.example.EchoInput
import smithy4s.example.EchoBody
import smithy4s.example.Podcast
import cats.kernel.Eq

object MonocleSpec extends FunSuite {

  implicit val echoInputEq: Eq[EchoInput] = Eq.fromUniversalEquals
  implicit val podVideoEq: Eq[Podcast] = Eq.fromUniversalEquals

  test("Lens transformation and composition") {
    val input = EchoInput("test", EchoBody(Some("test body")))
    val lens = EchoInput.Lenses.body.andThen(EchoBody.Lenses.data)
    val resultGet = lens.get(input)

    val resultSet =
      lens
        .replace(Some("new body"))(input)

    val updatedInput = EchoInput("test", EchoBody(Some("new body")))
    expect.eql(Some("test body"), resultGet) &&
    expect.eql(updatedInput, resultSet)
  }

  test("Prism transformation and composition") {
    val input = Podcast.Video(Some("Pod Title"))

    val prism = Podcast.Prisms.video.andThen(Podcast.Video.Lenses.title)
    val result = prism.replace(Some("New Pod Title"))(input)

    expect.eql(Podcast.Video(Some("New Pod Title")), result)
  }
}
