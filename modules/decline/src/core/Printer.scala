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

package smithy4s.decline.core

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
      codecs: CodecAPI
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
