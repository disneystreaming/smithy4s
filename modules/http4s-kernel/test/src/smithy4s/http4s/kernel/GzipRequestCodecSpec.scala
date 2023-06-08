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
            // echo "data" | gzip | base64
            // H4sIAAAAAAAAA0tJLEnkAgCCxcHmBQAAAA==
            // trailing equals are padding
            val checkRecordedBody = received.get.map { payload =>
              expect(payload == "H4sIAAAAAAAAB0tJLEkEAGPz860EAAAA")
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
            // echo "" | gzip | base64
            // H4sIAAAAAAAAA+MCAJMG1zIBAAAA
            val checkRecordedBody = received.get.map { payload =>
              expect(payload == "H4sIAAAAAAAABwMAAAAAAAAAAAA=")
            }
            List(checkRecordedBody, checkResponse).combineAll
        }
    }
  }
}
