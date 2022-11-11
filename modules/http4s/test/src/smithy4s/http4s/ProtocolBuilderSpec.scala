package smithy4s.http4s

import cats.effect.IO
import org.http4s.HttpApp
import org.http4s.client.Client
import smithy4s.example.PizzaAdminServiceGen
import smithy4s.example.WeatherGen
import weaver._

object ProtocolBuilderSpec extends FunSuite {

  private val fakeClient = Client.fromHttpApp(HttpApp.notFound[IO])

  test(
    "SimpleProtocolBuilder (client) fails when the protocol is not present"
  ) {
    val result = SimpleRestJsonBuilder(WeatherGen)
      .client(fakeClient)
      .use

    assert(result.isLeft)
  }

  test(
    "SimpleProtocolBuilder (client) succeeds when the protocol is present"
  ) {
    val result = SimpleRestJsonBuilder(PizzaAdminServiceGen)
      .client(fakeClient)
      .use

    assert(result.isRight)
  }

}
