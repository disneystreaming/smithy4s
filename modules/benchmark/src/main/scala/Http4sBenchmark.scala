/*
 *  Copyright 2021-2022 Disney Streaming
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

package smithy4s.benchmark

import cats.effect.IO
import cats.effect.Ref
import cats.effect.unsafe.implicits.global
import cats.syntax.all._
import io.circe.Json
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.openjdk.jmh.annotations._

import java.util.concurrent.TimeUnit
import scala.util.Random

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 30, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 60, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Fork(5)
class Http4sBenchmark {

  import Circe._

  val inputJson = s3ObjectCodec.apply(Payload.input)

  val genericRouter = smithy4s.http4s.RestJsonBuilder
    .routes(
      BenchmarkServiceHttp4sImpl(Ref.unsafe(Set.empty))
    )
    .make
    .toTry
    .get
    .orNotFound

  val handcraftedRouter = {
    val benchmarkServiceImpl = BenchmarkServiceHttp4sImpl(Ref.unsafe(Set.empty))

    HttpRoutes
      .of[IO] { case req @ POST -> Root / "simple" / bucketName / key =>
        for {
          json <- req.json
          payload <- IO.fromEither(json.as[String])
          _ <- benchmarkServiceImpl.sendString(
            key,
            bucketName,
            payload
          )
        } yield Response(Status.Ok)
      }
      .orNotFound
  }
  val genericRouterClient = Client.fromHttpApp[IO](genericRouter)
  val handcrafterRouterClient = Client.fromHttpApp[IO](handcraftedRouter)

  def randomString(length: Int): IO[String] = {
    IO.delay(Random.alphanumeric.take(length).mkString(""))
  }

  def createSimpleRequest(client: Client[IO]): IO[Unit] = {
    for {
      bucketName <- randomString(20)
      key <- randomString(20)
      request = Request[IO](POST)
        .withUri(
          Uri.unsafeFromString(s"http://localhost/simple/$bucketName/$key")
        )
        .withEntity(Json.fromString(Payload.loremIpsumString))
      st <- client.status(request)
      _ <- IO.raiseError(new Exception("Bad status")).unlessA(st.isSuccess)
    } yield ()
  }

  @Benchmark
  def measureHandcraftedApp(): Unit = {
    createSimpleRequest(handcrafterRouterClient).unsafeRunSync()
  }

  @Benchmark
  def measureGenericApp(): Unit = {
    createSimpleRequest(genericRouterClient).unsafeRunSync()
  }

}
