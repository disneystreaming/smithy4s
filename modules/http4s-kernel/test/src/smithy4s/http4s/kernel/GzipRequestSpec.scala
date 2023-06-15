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

package smithy4s.http4s.kernel

import cats.effect.IO
import cats.effect.kernel.Deferred
import cats.implicits._
import fs2.compression.DeflateParams
import org.http4s.HttpRoutes
import org.http4s.Method
import org.http4s.Request
import org.http4s.Response
import weaver._
import smithy4s.http4s.kernel.GzipRequestDecompression
import smithy4s.http4s.kernel.GzipRequestCompression

object GzipRequestSpec extends SimpleIOSuite with Compat {
  val compressor =
    GzipRequestCompression[IO](
      GzipRequestCompression.DefaultBufferSize,
      DeflateParams.Level.DEFAULT
    )
  val decompressor =
    GzipRequestDecompression[IO](
      GzipRequestDecompression.DefaultBufferSize
    )

  test("codecs roundtrip - compressor compress and decompressor decompress") {
    Deferred[IO, String].flatMap { received =>
      val routes = HttpRoutes.of[IO] { case req =>
        req.toStrict(None).flatMap { strict =>
          val record: IO[Unit] =
            strict.body
              .through(fs2.text.base64.encode)
              .compile
              .foldMonoid
              .flatMap { received.complete(_).void }
          val answer = decompressor(strict).as[String].map { payload =>
            Response[IO]().withEntity(payload)
          }
          record *> answer
        }
      }
      val originalPayload = "data"
      val request =
        Request[IO](method = Method.POST).withEntity(originalPayload)
      routes
        .run(compressor(request))
        .value
        .flatMap {
          case None => IO.pure(failure("expected a response"))
          case Some(fa) =>
            val checkResponse = fa
              .as[String]
              .map { payload => expect(payload == "data") }
            // local:   "H4sIAAAAAAAAB0tJLEkEAGPz860EAAAA"
            // ci:      "H4sIAAAAAAAAA0tJLEkEAGPz860EAAAA"
            // js:      "H4sIAAAAAAAAE0tJLEkEAGPz860EAAAA"
            val check = "^H4sIAAAAAAAA[ABE]0tJLEkEAGPz860EAAAA=*$".r
            val checkRecordedBody = received.get.map { payload =>
              expect(check.findFirstIn(payload) == Some(payload))
            }
            List(checkRecordedBody, checkResponse).combineAll
        }
    }
  }

  test("codecs roundtrip - works on empty body") {
    Deferred[IO, String].flatMap { received =>
      val routes = HttpRoutes.of[IO] { case req =>
        req.toStrict(None).flatMap { strict =>
          val record: IO[Unit] =
            strict.body
              .through(fs2.text.base64.encode)
              .compile
              .foldMonoid
              .flatMap { received.complete(_).void }
          val answer = decompressor(strict).as[String].map { payload =>
            Response[IO]().withEntity(payload)
          }
          record *> answer
        }
      }
      val request =
        Request[IO](method = Method.POST)
      routes
        .run(compressor(request))
        .value
        .flatMap {
          case None => IO.pure(failure("expected a response"))
          case Some(fa) =>
            val checkResponse = fa
              .as[String]
              .map { payload => expect(payload == "") }
            // local:   "H4sIAAAAAAAABwMAAAAAAAAAAAA"
            // ci:      "H4sIAAAAAAAAAwMAAAAAAAAAAAA="
            // js:      "H4sIAAAAAAAAEwMAAAAAAAAAAAA="
            val check = "^H4sIAAAAAAAA[ABE]wMAAAAAAAAAAAA=*$".r
            val checkRecordedBody = received.get.map { payload =>
              expect(check.findFirstIn(payload) == Some(payload))
            }
            List(checkRecordedBody, checkResponse).combineAll
        }
    }
  }
}
