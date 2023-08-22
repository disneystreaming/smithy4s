---
sidebar_label: Localstack
---

It is a common need to be able to test AWS operations on a local environment. For that, many engineers have turned to [Localstack](https://localstack.cloud/).

Smithy4s does not provide any utility method to allow for this, for the simple reason that it can be done reasonably easily
at the level of the underlying http client, by mean of a middleware.

### Implementation

In order to target an Smithy4s-built AWS client to a local environment, you need to create a middleware (ie a `Client[F] => Client[F]` function) that redirects requests to the Localstack host and port. Here's an example

```scala mdoc:compile-only
import cats.effect._
import cats.syntax.all._
import com.amazonaws.dynamodb._
import fs2.io.net.Network
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import fs2.compression.Compression
import org.http4s._
import org.typelevel.ci._
import smithy4s.aws._
import smithy4s.aws.kernel.AwsRegion

object LocalstackProxy {
  def apply[F[_]: Async: Compression](client: Client[F]): Client[F] = Client { req =>
    client.run(
      req.withUri(
        req.uri.copy(authority =
          req.uri.authority.map(x =>
            x.copy(
              host = Uri.RegName("localhost"),
              port = Some(4566)
            )
          )
        )
      )
      .putHeaders(Header.Raw(ci"host", "localhost"))
    )
  }
}

object LocalstackDynamoDB {
  def env[F[_]: Async: Compression](client: Client[F], region: AwsRegion): AwsEnvironment[F] = AwsEnvironment.make[F](
    client,
    Async[F].pure(region),
    Async[F].pure(AwsCredentials.Default("mock-key-id", "mock-secret-key", None)),
    Async[F].realTime.map(_.toSeconds).map(Timestamp(_, 0))
  )

  def client[F[_]: Async: Network: Compression](client: Client[F], region: AwsRegion): Resource[F, DynamoDB.Impl[F]] =
    AwsClient(DynamoDB.service, env[F](LocalstackProxy[F](client), region))
}

def myResource[F[_]: Async: Network: Compression] = for {
  underlying <- EmberClientBuilder
    .default[F]
    .withoutCheckEndpointAuthentication
    .build
  client <- LocalstackDynamoDB.client[F](underlying, AwsRegion.US_EAST_1)
} yield client
```
