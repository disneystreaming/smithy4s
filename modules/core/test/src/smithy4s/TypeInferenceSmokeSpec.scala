package smithy4s

import weaver._
import smithy4s.example._
import cats.Functor
import cats.syntax.all._
import cats.effect.IO

object TypeInferenceSmokeSpec extends SimpleIOSuite {

  test("Type inference works with service calls") {
    /*
     * Checks that `map` can be called without upcasting the result of
     * the service call to F[something].
     */
    def foo[F[_]: Functor](dummyService: DummyService[F]): F[Int] =
      dummyService.dummy().map(_ => 1)

    val dummyInstance = new DummyService[IO] {

      override def dummy(
          str: Option[String],
          int: Option[Int],
          ts1: Option[Timestamp],
          ts2: Option[Timestamp],
          ts3: Option[Timestamp],
          ts4: Option[Timestamp],
          b: Option[Boolean],
          sl: Option[List[String]],
          slm: Option[Map[String, List[String]]]
      ): IO[Unit] = IO.unit

    }
    foo(dummyInstance).map(x => expect(x == 1))
  }

}
