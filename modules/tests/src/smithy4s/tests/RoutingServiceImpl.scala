package smithy4s.tests

import smithy4s.example.RoutingService
import cats.effect.IO
import smithy4s.example.MessageOutput


class RoutingServiceImpl extends RoutingService[IO] {

  override def greedyAbcDef(abc: String): IO[MessageOutput] = IO.pure(MessageOutput("greedyAbcDef"))

  override def abcLabel(_def: String): IO[MessageOutput] = IO.pure(MessageOutput("abcLabel"))

  override def abcDef(): IO[MessageOutput] = IO.pure(MessageOutput("abcDef"))

  override def abc(): IO[MessageOutput] = IO.pure(MessageOutput("abc"))
  
}
