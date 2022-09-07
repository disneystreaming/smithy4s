package smithy4s.weavertests

import smithy4s.example._
import smithy4s.http4s._
import cats.effect.IO

object WeaverTestTests
    extends WeaverTests(
      smithy4s.api.SimpleRestJson(),
      SimpleRestJsonBuilder(HelloServiceGen).client[IO],
      SimpleRestJsonBuilder.routes(_: HelloService[IO]).make
    ) {
  recordTests(new CompatEffect)
}
