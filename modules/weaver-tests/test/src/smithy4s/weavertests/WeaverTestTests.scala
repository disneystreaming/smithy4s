package smithy4s.weavertests

import smithy4s.example._
import smithy4s.http4s._
import cats.effect.IO
import smithy4s.ShapeTag

object WeaverTestTestsExample
    extends WeaverTests(
      ShapeTag[smithy4s.api.SimpleRestJson],
      SimpleRestJsonBuilder(HelloServiceGen).client[IO],
      SimpleRestJsonBuilder.routes(_: HelloService[IO]).make
    )
