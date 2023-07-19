package smithy4s.optics

import munit._
import smithy4s.example.{EchoInput, EchoBody, Podcast}

final class CompositionSpec extends FunSuite {

  test("Lens transformation and composition") {
    val input = EchoInput("test", EchoBody(Some("test body")))
    val lens = EchoInput.Lenses.body.andThen(EchoBody.Lenses.data)
    val resultGet = lens.get(input)

    val resultSet =
      lens
        .replace(Some("new body"))(input)

    val updatedInput = EchoInput("test", EchoBody(Some("new body")))
    assertEquals(Option("test body"), resultGet)
    assertEquals(updatedInput, resultSet)
  }

  test("Prism transformation and composition") {
    val input = Podcast.Video(Some("Pod Title"))

    val prism = Podcast.Prisms.video.andThen(Podcast.Video.Lenses.title)
    val result = prism.replace(Some("New Pod Title"))(input)

    assertEquals(Podcast.Video(Some("New Pod Title")).widen, result)
  }

}
