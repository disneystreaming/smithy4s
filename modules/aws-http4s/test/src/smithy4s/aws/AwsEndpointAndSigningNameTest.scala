/*
 *  Copyright 2021-2026 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package smithy4s.aws

import cats.effect.IO
import cats.effect.Resource
import cats.syntax.all._
import com.amazonaws.dynamodb.DynamoDB
import org.http4s.Request
import org.http4s.Response
import org.http4s.Status
import org.http4s.client.Client
import smithy4s.aws.kernel.AwsCredentials
import smithy4s.aws.kernel.Timestamp
import weaver._

/**
  * The host and the sigv4 signing name are resolved from *different* fields and
  * are not interchangeable: SES v2 is reached at `email.<region>.amazonaws.com`
  * but must be signed as `ses`, or AWS rejects the request with "Credential
  * should be scoped to correct service".
  *
  * These tests pin both halves for each combination of `endpointPrefix`,
  * `arnNamespace` and `aws.auth#sigv4` we have seen in the wild.
  */
object AwsEndpointAndSigningNameTest extends SimpleIOSuite with Compat {

  private val credentials = AwsCredentials.Default(
    accessKeyId = "AKIAIOSFODNN7EXAMPLE",
    secretAccessKey = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY",
    sessionToken = None
  )

  /** 2024-09-27T00:00:00Z -- fixed so the credential scope is deterministic. */
  private val timestamp = Timestamp(1727395200L, 0)

  /**
    * Runs one operation against a stub client and returns the request that
    * would have gone out to AWS.
    */
  private def capture(
      run: AwsEnvironment[IO] => Resource[IO, Any]
  ): IO[Request[IO]] =
    IO.ref(Option.empty[Request[IO]]).flatMap { ref =>
      val httpClient = Client[IO] { req =>
        Resource.eval(ref.set(Some(req))).as(Response[IO](Status.Forbidden))
      }

      val awsEnv = AwsEnvironment.make[IO](
        httpClient,
        IO.pure(AwsRegion.US_EAST_1),
        IO.pure(credentials),
        IO.pure(timestamp)
      )

      run(awsEnv).use_.attempt *>
        ref.get.flatMap(
          _.liftTo[IO](new AssertionError("no request was sent"))
        )
    }

  private def host(request: Request[IO]): Option[String] =
    request.uri.host.map(_.renderString)

  /** The `<service>` part of `Credential=<key>/<date>/<region>/<service>/aws4_request`. */
  private def signingName(request: Request[IO]): Option[String] =
    request.headers
      .get(org.typelevel.ci.CIString("Authorization"))
      .map(_.head.value)
      .flatMap { auth =>
        "Credential=[^/]+/[^/]+/[^/]+/([^/]+)/aws4_request".r
          .findFirstMatchIn(auth)
          .map(_.group(1))
      }

  test(
    "endpointPrefix and sigv4 name differ: host uses the prefix, signing uses sigv4 (SES v2)"
  ) {
    capture { awsEnv =>
      AwsClient(smithy4s.example.aws.PrefixDiffersFromSigningName, awsEnv)
        .evalMap(_.doThing())
    }.map { request =>
      expect.same(host(request), Some("email.us-east-1.amazonaws.com")) &&
      expect.same(signingName(request), Some("ses"))
    }
  }

  test(
    "dotted endpointPrefix: host keeps the dots, signing uses sigv4 (SageMaker, #1568)"
  ) {
    capture { awsEnv =>
      AwsClient(smithy4s.example.aws.DottedPrefix, awsEnv)
        .evalMap(_.doThing())
    }.map { request =>
      expect.same(
        host(request),
        Some("api.sagemaker.us-east-1.amazonaws.com")
      ) &&
      expect.same(signingName(request), Some("sagemaker"))
    }
  }

  test(
    "no endpointPrefix: both fall back to arnNamespace, never the operation name (Account, #1532)"
  ) {
    capture { awsEnv =>
      AwsClient(smithy4s.example.aws.NoEndpointPrefix, awsEnv)
        .evalMap(_.doThing())
    }.map { request =>
      expect.same(host(request), Some("account.us-east-1.amazonaws.com")) &&
      expect.same(signingName(request), Some("account"))
    }
  }

  test(
    "no sigv4 trait: signing falls back to arnNamespace rather than endpointPrefix"
  ) {
    capture { awsEnv =>
      AwsClient(smithy4s.example.aws.NoSigv4, awsEnv)
        .evalMap(_.doThing())
    }.map { request =>
      expect.same(
        host(request),
        Some("endpointprefix.us-east-1.amazonaws.com")
      ) &&
      expect.same(signingName(request), Some("arnnamespace"))
    }
  }

  test("all three names agree: nothing changes (DynamoDB)") {
    capture { awsEnv =>
      AwsClient(DynamoDB, awsEnv).evalMap(_.listTables())
    }.map { request =>
      expect.same(host(request), Some("dynamodb.us-east-1.amazonaws.com")) &&
      expect.same(signingName(request), Some("dynamodb"))
    }
  }

}
