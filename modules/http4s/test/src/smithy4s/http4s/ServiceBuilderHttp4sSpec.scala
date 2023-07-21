package smithy4s.http4s

import cats.effect.IO
import smithy4s.example.guides.auth.{HealthCheckOutput, HelloWorldAuthServiceGen}
import smithy4s.kinds.PolyFunction5
import smithy4s.{Endpoint, Hints, Service}
import smithy4s.example.guides.auth.World
import smithy4s.example.guides.auth.HelloWorldAuthService
import org.http4s.implicits._
import org.http4s.Request
import weaver.SimpleIOSuite

object ServiceBuilderHttp4sSpec  extends SimpleIOSuite{

  val serviceImpl: HelloWorldAuthService[IO] = new HelloWorldAuthService[IO] {
    override def sayWorld(): IO[World] = IO.pure(World("hello"))

    override def healthCheck(): IO[HealthCheckOutput] = ???
 }


  val builder = Service.Builder.fromService(HelloWorldAuthService)

  val mapper = new PolyFunction5[HelloWorldAuthServiceGen.Endpoint, HelloWorldAuthServiceGen.Endpoint] {
    def apply[I, E, O, SI, SO](op: HelloWorldAuthServiceGen.Endpoint[I, E, O, SI, SO]): HelloWorldAuthServiceGen.Endpoint[I, E, O, SI, SO] = {
      if(op.name == "SayWorld") {
        Endpoint
          .Builder
          .fromEndpoint(op)
          .withHints(
            Hints(
              smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/yeap"), code = 200),
              smithy.api.Readonly(),
            )
          )
          .build
      } else  {
        op
      }
    }
  }
  val modifiedService = builder
    .mapEndpointEach(mapper)
    .build

  test("can modify the uri path of an endpont") {
    SimpleRestJsonBuilder(modifiedService)
      .routes(serviceImpl)
      .resource
      .use { routes =>
        routes.orNotFound.run(Request[IO](uri = uri"/yeap")).map { response =>
          assert(response.status.code == 200)
        }
      }
  }
}
