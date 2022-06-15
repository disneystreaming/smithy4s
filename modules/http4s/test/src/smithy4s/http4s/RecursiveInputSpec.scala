package smithy4s.http4s

import weaver._

import org.http4s.HttpApp
import cats.effect.IO
import org.http4s.Uri

// This is a non-regression test for https://github.com/disneystreaming/smithy4s/issues/181
object RecursiveInputSpec extends FunSuite {

  test("simpleRestJson works with recursive input operations") {
    val result =
      SimpleRestJsonBuilder(smithy4s.example.RecursiveInputService).client(
        HttpApp.notFound[IO],
        Uri.unsafeFromString("http://localhost")
      )

    expect(result.isRight)
  }

}
