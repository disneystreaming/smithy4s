import smithy4s.hello._
import cats.effect._
import cats.implicits._
import org.http4s.implicits._
import org.http4s.ember.server._
import org.http4s._
import com.comcast.ip4s._
import smithy4s.http4s.SimpleRestJsonBuilder

object HelloWorldServiceImpl extends HelloWorldService[IO] {
  def hello(name: String, town: Option[String]): IO[Greeting] =
    IO.pure(Greeting("yeah!"))
}

object Routes {
  private val helloRoutes: Resource[IO, HttpRoutes[IO]] =
    SimpleRestJsonBuilder.routes(HelloWorldServiceImpl).resource

  private val docs: HttpRoutes[IO] =
    smithy4s.http4s.swagger
      .docs[IO](HelloWorldService, smithy4s.example.PizzaAdminService)

  val all: Resource[IO, HttpRoutes[IO]] = helloRoutes.map(_ <+> docs)
}

object Main extends IOApp.Simple {
  val run = Routes.all.flatMap { routes =>
    EmberServerBuilder
      .default[IO]
      .withPort(port"8080")
      .withHost(host"0.0.0.0")
      .withHttpApp(routes.orNotFound)
      .build
  }.useForever

}
