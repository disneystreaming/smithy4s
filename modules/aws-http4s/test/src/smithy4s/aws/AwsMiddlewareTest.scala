package smithy4s.aws

import weaver._
import cats.effect.IO
import org.http4s.Request
import org.http4s.Response
import org.http4s.Status
import org.http4s.client.Client
import com.amazonaws.dynamodb.DynamoDB
import _root_.smithy4s.Endpoint
import _root_.smithy4s.Service
import org.http4s.Headers
import org.typelevel.ci.CIString

object AwsMiddlewareTest extends SimpleIOSuite with Compat {

  test("Endpoint middleware can be applied on AWS Clients") {
    val epMiddleware = new Endpoint.Middleware[Client[IO]] {
      def prepare[Alg[_[_, _, _, _, _]]](
          service: Service[Alg]
      )(endpoint: service.Endpoint[_, _, _, _, _]): Client[IO] => Client[IO] =
        underlying =>
          Client { req =>
            underlying.run(
              req.transformHeaders(_ ++ Headers("X-Service" -> service.id.name))
            )
          }
    }
    val test = for {
      ref <- IO.ref[Option[Request[IO]]](None).toResource
      httpClient = Client[IO](req =>
        ref.set(Some(req)).as(Response[IO](Status.Forbidden)).toResource
      )
      awsEnv <- AwsEnvironment
        .default(httpClient, AwsRegion.US_EAST_1)
        .map(_.withMiddleware(epMiddleware))
      awsClient <- AwsClient(DynamoDB, awsEnv)
      _ <- awsClient.listTables().attempt.toResource
      interceptedHeaders <- ref.get
        .map(_.map(_.headers).getOrElse(Headers.empty))
        .toResource
    } yield {
      val xServiceHeader =
        interceptedHeaders.get(CIString("X-Service")).map(_.head.value)
      expect.same(xServiceHeader, Some("DynamoDB_20120810"))
    }
    test.use(IO.pure)
  }

}
