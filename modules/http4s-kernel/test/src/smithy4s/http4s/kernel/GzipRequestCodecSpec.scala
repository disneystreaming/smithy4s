package smithy4s.http4s.kernel

import weaver._
import cats.implicits._
import org.http4s.HttpRoutes
import cats.effect.IO
import cats.effect.kernel.Deferred
import org.http4s.Response
import fs2.compression.DeflateParams
import org.http4s.Method
import org.http4s.Request

object GzipRequestCodecSpec extends SimpleIOSuite {
  private val stringCodecs = {
    val encoder =
      GzipRequestEncoder.make[IO, String](
        GzipRequestEncoder.DefaultBufferSize,
        DeflateParams.Level.DEFAULT
      )
    val internal: RequestDecoder[IO, String] =
      new RequestDecoder[IO, String]() {
        def decodeRequest(request: Request[IO]): IO[String] = {
          request.as[String]
        }
      }
    val decoder =
      GzipRequestDecoder.make[IO, String](
        GzipRequestDecoder.DefaultBufferSize
      )(internal)
    (encoder, decoder)
  }

  test("codecs roundtrip - encoder compress and decoder decompress") {
    val (encoder, decoder) = stringCodecs
    Deferred[IO, String].flatMap { received =>
      val routes = HttpRoutes.of[IO] { case req =>
        req.toStrict(None).flatMap { strict =>
          val record: IO[Unit] =
            strict.body
              .through(fs2.text.base64.encode)
              .compile
              .foldMonoid
              .flatMap { received.complete(_).void }
          val answer = decoder.decodeRequest(strict).map { payload =>
            Response[IO]().withEntity(payload)
          }
          record *> answer
        }
      }
      val originalPayload = "data"
      val request =
        Request[IO](method = Method.POST).withEntity(originalPayload)
      routes
        .run(encoder.addToRequest(request, ""))
        .value
        .flatMap {
          case None => IO.pure(failure("expected a response"))
          case Some(fa) =>
            val checkResponse = fa
              .as[String]
              .map { payload => expect(payload == "data") }
            // local:   "H4sIAAAAAAAAB0tJLEkEAGPz860EAAAA"
            // ci:      "H4sIAAAAAAAAA0tJLEkEAGPz860EAAAA"
            val check = "^H4sIAAAAAAAA[AB]0tJLEkEAGPz860EAAAA=*$".r
            val checkRecordedBody = received.get.map { payload =>
              expect(check.findFirstIn(payload) == Some(payload))
            }
            List(checkRecordedBody, checkResponse).combineAll
        }
    }
  }

  test("codecs roundtrip - works on empty body") {
    val (encoder, decoder) = stringCodecs
    Deferred[IO, String].flatMap { received =>
      val routes = HttpRoutes.of[IO] { case req =>
        req.toStrict(None).flatMap { strict =>
          val record: IO[Unit] =
            strict.body
              .through(fs2.text.base64.encode)
              .compile
              .foldMonoid
              .flatMap { received.complete(_).void }
          val answer = decoder.decodeRequest(strict).map { payload =>
            Response[IO]().withEntity(payload)
          }
          record *> answer
        }
      }
      val request =
        Request[IO](method = Method.POST)
      routes
        .run(encoder.addToRequest(request, ""))
        .value
        .flatMap {
          case None => IO.pure(failure("expected a response"))
          case Some(fa) =>
            val checkResponse = fa
              .as[String]
              .map { payload => expect(payload == "") }
            // local:   "H4sIAAAAAAAABwMAAAAAAAAAAAA"
            // ci:      "H4sIAAAAAAAAAwMAAAAAAAAAAAA="
            val check = "^H4sIAAAAAAAA[AB]wMAAAAAAAAAAAA=*$".r
            val checkRecordedBody = received.get.map { payload =>
              expect(check.findFirstIn(payload) == Some(payload))
            }
            List(checkRecordedBody, checkResponse).combineAll
        }
    }
  }
}
