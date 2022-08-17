package smithy4s.cli.core

import cats.Applicative
import cats.effect.std.Console
import cats.implicits._
import smithy4s.Endpoint
import smithy4s.http.CodecAPI

trait Printer[F[_], -I, -O] {
  def printInput(input: I): F[Unit]
  def printError(error: Throwable): F[Unit]
  def printOutput(output: O): F[Unit]
}

object Printer {

  def fromCodecs[F[_]: Console: Applicative, Op[_, _, _, _, _], I, O](
    endpoint: Endpoint[Op, I, _, O, _, _],
    codecs: CodecAPI,
  ): Printer[F, I, O] =
    new Printer[F, I, O] {
      private val outCodec = codecs.compileCodec(endpoint.output)

      private val errCodec = endpoint.errorable.map { e =>
        (codecs.compileCodec(e.error), e)
      }

      def printInput(input: I): F[Unit] = Applicative[F].unit

      def printError(error: Throwable): F[Unit] = errCodec
        .flatMap { case (err, errorable) =>
          errorable.liftError(error).map { e =>
            new String(codecs.writeToArray(err, e))
          }
        }
        .traverse_(Console[F].println(_))

      def printOutput(output: O): F[Unit] = Console[F].println {
        new String(codecs.writeToArray(outCodec, output))
      }

    }

}
