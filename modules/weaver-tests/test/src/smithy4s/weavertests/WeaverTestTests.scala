package smithy4s.weavertests

import smithy4s.example._
import smithy4s.http4s._
import cats.effect.IO

object WeaverTestTestsExample
    extends WeaverTests(
      SimpleRestJsonBuilder(HelloServiceGen).client[IO],
      SimpleRestJsonBuilder.routes(_: HelloService[IO]).make
    )
