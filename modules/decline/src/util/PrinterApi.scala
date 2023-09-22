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

package smithy4s.decline.util

import cats.Applicative
import cats.effect.std.Console
import cats.implicits._
import com.monovore.decline.Opts
import smithy4s.Endpoint
import smithy4s.decline.core.Printer
import smithy4s.codecs._

import scala.{Console => SConsole}
import com.monovore.decline.Argument

trait PrinterApi[F[_]] {

  def printer[Op[_, _, _, _, _], I, O](
      endpoint: Endpoint[Op, I, _, O, _, _]
  ): Printer[F, I, O]

}

object PrinterApi {

  object opts {

    def default[F[_]: Console: Applicative](
        extraCodecs: (String, PrinterApi[F])*
    ): Opts[PrinterApi[F]] = fromMap(Map("json" -> json[F]) ++ extraCodecs)
      .withDefault(PrinterApi.std)

    def fromMap[F[_]](
        printers: Map[String, PrinterApi[F]]
    ): Opts[PrinterApi[F]] = {
      implicit val arg: Argument[PrinterApi[F]] = Argument.fromMap(
        "output",
        printers
      )

      Opts.option[PrinterApi[F]]("output", "Output mode")
    }

  }

  def json[F[_]: Console: Applicative]: PrinterApi[F] = useCodec(
    smithy4s.json.Json.payloadCodecs.writers
  )

  def useCodec[F[_]: Console: Applicative](
      codec: PayloadEncoder.CachedCompiler
  ): PrinterApi[F] =
    new PrinterApi[F] {

      def printer[Op[_, _, _, _, _], I, O](
          endpoint: Endpoint[Op, I, _, O, _, _]
      ): Printer[F, I, O] = Printer.fromCodecs(endpoint, codec)

    }

  def std[F[_]: Console]: PrinterApi[F] =
    new PrinterApi[F] {

      def printer[Op[_, _, _, _, _], I, O](
          endpoint: Endpoint[Op, I, _, O, _, _]
      ): Printer[F, I, O] =
        new Printer[F, I, O] {

          def printInput(
              input: I
          ): F[Unit] =
            Console[F].println(s"${SConsole.BLUE}> $input${SConsole.RESET}")

          def printError(error: Throwable): F[Unit] = Console[F]
            .errorln(s"${SConsole.RED}< $error${SConsole.RESET}")

          def printOutput(
              output: O
          ): F[Unit] =
            Console[F].println(s"${SConsole.GREEN}< $output${SConsole.RESET}")

        }

    }

}
