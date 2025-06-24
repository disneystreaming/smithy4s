package smithy4s.tests

import cats.effect._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import weaver._
import org.http4s.client.Client
import org.http4s.Uri
import cats.Show
import org.http4s.Request
import smithy4s.example.RoutingService


abstract class RoutingSpec 
    extends IOSuite
    with Http4sDsl[IO]
    with Http4sClientDsl[IO] {

  type Res = (Client[IO], Uri)
  
  def runServer(
    routingService: RoutingService[IO],
    errorAdapter: PartialFunction[Throwable, Throwable]
  ): Resource[IO, Res]

  override def sharedResource: Resource[IO, Res] = 
    runServer(new RoutingServiceImpl(), PartialFunction.empty[Throwable, Throwable])

  routerTest("/abc") {
    (client, uri) =>
      for {
        res <- client.send[String](GET((uri / "abc")))
      } yield {
        val (code, body) = res
        expect.same(code, 200) && expect(body == "\"abc\"")
      }
  }

  routerTest("/abc/def") {
    (client, uri) =>
      for {
        res <- client.send[String](GET((uri / "abc" / "def")))
      } yield {
        val (code, body) = res
        expect.same(code, 200) && expect(body == "\"abcDef\"")
      }
  }

  routerTest("/abc/{def}") {
    (client, uri) =>
      for {
        res <- client.send[String](GET((uri / "abc" / "notDef")))
      } yield {
        val (code, body) = res
        expect.same(code, 200) && expect(body == "\"abcLabel\"")
      }
  }


  routerTest("/{abc+}/def") {
    (client, uri) =>
      for {
        res <- client.send[String](GET((uri / "abc" / "def" / "def")))
      } yield {
        val (code, body) = res
        expect.same(code, 200) && expect(body == "\"greedyAbcDef\"")
      }
  }

    def routerTest(testName: TestName)(
      f: (Client[IO], Uri) => IO[Expectations]
  ) = test(testName)((res: Res) => f(res._1, res._2))

  implicit class ClientOps(client: Client[IO]) {
    // Returns: (status, body string)
    def send[A: Show](request: Request[IO]): IO[(Int, String)] =
      client.run(request).use { response =>
        val code = response.status.code
        response.as[String].map(code -> _)
      }

  }
}
