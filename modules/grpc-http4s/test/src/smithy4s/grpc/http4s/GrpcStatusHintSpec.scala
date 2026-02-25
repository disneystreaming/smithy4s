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

package smithy4s.grpc.http4s

import cats.effect.IO
import cats.effect.kernel.Ref
import cats.effect.kernel.Resource
import org.http4s.{Response, Status, Uri}
import org.http4s.client.Client
import smithy4s.example.grpc._
import smithy4s.grpc.GrpcFailure
import smithy4s.grpc.GrpcMessageEncoding
import smithy4s.grpc.internals.GrpcConstants
import smithy4s.kinds.PolyFunction5
import weaver.{Expectations, SimpleIOSuite}

object GrpcStatusHintSpec extends SimpleIOSuite {

  private val Impl: GrpcGreetingServiceGen.Impl[IO] =
    GrpcGreetingServiceGen.fromPolyFunction(
      new PolyFunction5[GrpcGreetingServiceOperation, smithy4s.kinds.Kind1[IO]#toKind5] {
        def apply[I, E, O, SI, SO](op: GrpcGreetingServiceOperation[I, E, O, SI, SO]): IO[O] =
          op match {
            case GrpcGreetingServiceOperation.Greet(GreetInput(name)) =>
              (name match {
                case "missing" => IO.raiseError(NotFoundError("not found"))
                case "denied"  => IO.raiseError(PermissionDeniedError("permission denied"))
                case _         => IO.pure(GreetOutput(s"Hello, $name!"))
              }).asInstanceOf[IO[O]]
          }
      }
    )

  private val builder = GrpcProtocolBuilder.withRequireHttp2(false)

  // Resource providing both the typed client and an intercepted last response,
  // so raw HTTP assertions can be made on the response produced by the server.
  private def withClientAndResponse(
      f: (GrpcGreetingServiceGen.Impl[IO], Ref[IO, Option[Response[IO]]]) => IO[Expectations]
  ): IO[Expectations] =
    builder(GrpcGreetingServiceGen).routes(Impl).resource.use { httpRoutes =>
      Ref[IO].of(Option.empty[Response[IO]]).flatMap { lastResponse =>
        val intercepting: Client[IO] = Client { req =>
          Resource.eval(
            httpRoutes.orNotFound.run(req).flatMap { resp =>
              lastResponse.set(Some(resp)).as(resp)
            }
          )
        }
        builder(GrpcGreetingServiceGen)
          .client(intercepting)
          .uri(Uri.unsafeFromString("http://localhost"))
          .make
          .fold(
            err => IO.raiseError(new RuntimeException(err.getMessage)),
            client => f(client, lastResponse)
          )
      }
    }

  test("typed: NotFoundError round-trips via grpc-status 5") {
    withClientAndResponse { (client, _) =>
      client.greet("missing").attempt.map {
        case Left(e: NotFoundError) => expect.eql(e.message, "not found")
        case Left(e: GrpcFailure)   => failure(s"unexpected GrpcFailure with status ${e.status}")
        case Left(e)                => failure(s"unexpected error: $e")
        case Right(_)               => failure("expected error, got success")
      }
    }
  }

  test("typed: PermissionDeniedError round-trips via grpc-status 7") {
    withClientAndResponse { (client, _) =>
      client.greet("denied").attempt.map {
        case Left(e: PermissionDeniedError) => expect.eql(e.message, "permission denied")
        case Left(e: GrpcFailure)           => failure(s"unexpected GrpcFailure with status ${e.status}")
        case Left(e)                        => failure(s"unexpected error: $e")
        case Right(_)                       => failure("expected error, got success")
      }
    }
  }

  test("typed: successful response decodes correctly") {
    withClientAndResponse { (client, _) =>
      client.greet("World").map { output =>
        expect.eql(output.greeting, "Hello, World!")
      }
    }
  }

  test("raw: NotFoundError response has HTTP 200 and grpc-status trailer 5") {
    withClientAndResponse { (client, lastResponse) =>
      client.greet("missing").attempt >> lastResponse.get.flatMap {
        case None => IO.pure(failure("no response captured"))
        case Some(response) =>
          response.trailerHeaders.map { trailers =>
            val grpcStatus = trailers
              .get(org.typelevel.ci.CIString(GrpcConstants.grpcStatusHeader))
              .map(_.head.value)
            val grpcMessage = trailers
              .get(org.typelevel.ci.CIString(GrpcConstants.grpcMessageHeader))
              .map(_.head.value)
              .flatMap(GrpcMessageEncoding.decode(_).toOption)
            expect.eql(response.status, Status.Ok) &&
            expect.eql(grpcStatus, Some("5")) &&
            expect.eql(grpcMessage, Some("Resource not found"))
          }
      }
    }
  }

  test("raw: PermissionDeniedError response has HTTP 200 and grpc-status trailer 7") {
    withClientAndResponse { (client, lastResponse) =>
      client.greet("denied").attempt >> lastResponse.get.flatMap {
        case None => IO.pure(failure("no response captured"))
        case Some(response) =>
          response.trailerHeaders.map { trailers =>
            val grpcStatus = trailers
              .get(org.typelevel.ci.CIString(GrpcConstants.grpcStatusHeader))
              .map(_.head.value)
            val grpcMessage = trailers
              .get(org.typelevel.ci.CIString(GrpcConstants.grpcMessageHeader))
              .map(_.head.value)
              .flatMap(GrpcMessageEncoding.decode(_).toOption)
            expect.eql(response.status, Status.Ok) &&
            expect.eql(grpcStatus, Some("7")) &&
            expect.eql(grpcMessage, Some("Permission denied"))
          }
      }
    }
  }

  test("raw: successful response has HTTP 200 and grpc-status trailer 0") {
    withClientAndResponse { (client, lastResponse) =>
      client.greet("World") >> lastResponse.get.flatMap {
        case None => IO.pure(failure("no response captured"))
        case Some(response) =>
          response.trailerHeaders.map { trailers =>
            val grpcStatus = trailers
              .get(org.typelevel.ci.CIString(GrpcConstants.grpcStatusHeader))
              .map(_.head.value)
            expect.eql(response.status, Status.Ok) &&
            expect.eql(grpcStatus, Some("0"))
          }
      }
    }
  }
}
