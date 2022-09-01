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

package smithy4s.decline

import cats.effect.ExitCode
import cats.effect.IO
import cats.implicits._
import com.monovore.decline.Opts
import com.monovore.decline.Command
import smithy.api.Documentation
import smithy.api.ExternalDocumentation
import smithy.api.Http
import smithy4s.Endpoint
import smithy4s.GenLift
import smithy4s.Monadic
import smithy4s.Service
import smithy4s.decline.core._
import smithy4s.decline.util.PrinterApi
import smithy4s.http.HttpEndpoint

import commons._

final case class Entrypoint[Alg[_[_, _, _, _, _]], F[_]](
    interpreter: Monadic[Alg, F],
    printerApi: PrinterApi[F]
)

/** Main entrypoint to Smithy4s CLIs. For convenience, see other modules like smithy4s-decline-ember. //
  * @param mainOpts
  *   Opts providing an interpreter to execut commands, and printer to use when displaying the
  *   input/output/errors. See [[smithy4s.decline.util.PrinterApi]] for default options
  * @param service
  *   The service to build a client call for
  */
class Smithy4sCli[Alg[_[_, _, _, _, _]], Op[_, _, _, _, _]](
    mainOpts: Opts[Entrypoint[Alg, IO]],
    service: Service[Alg, Op]
) {

  private def protocolSpecificHelp(
      endpoint: Endpoint[Op, _, _, _, _, _]
  ): List[String] =
    HttpEndpoint
      .cast(endpoint)
      .map { httpEndpoint =>
        val path = endpoint.hints
          .get(Http)
          .map(_.uri.value)
          .getOrElse(
            throw new Exception(
              "HTTP hint missing in a HTTP endpoint! This is probably a bug in Smithy4s codegen."
            )
          )

        s"HTTP ${httpEndpoint.method.showUppercase} $path"
      }
      .toList

  private def makeHelpBlocks(
      endpoint: Endpoint[Op, _, _, _, _, _]
  ): List[String] =
    protocolSpecificHelp(endpoint) ++
      endpoint.hints.get[Documentation].map(_.value) ++
      endpoint.hints
        .get[ExternalDocumentation]
        .map(
          _.value
            .map { case (url, description) => s"$url: $description" }
            .mkString("\n")
        )

  private def endpointSubcommand[I, E, O](
      endpoint: Endpoint[Op, I, E, O, _, _]
  ): Opts[IO[ExitCode]] = {

    def compileToOpts[A](schema: smithy4s.Schema[A]): Opts[A] =
      schema.compile[Opts](OptsVisitor)

    val inputOpts: Opts[I] = compileToOpts(endpoint.input)

    Opts
      .subcommand(
        name = toKebabCase(endpoint.name),
        help = makeHelpBlocks(endpoint).mkString_("\n\n")
      ) {
        (
          inputOpts,
          mainOpts
        ).mapN { (input, entrypoint) =>
          val printers = entrypoint.printerApi
          val printer = printers.printer(endpoint)
          printer.printInput(input) *>
            service
              .asTransformation[GenLift[IO]#λ](entrypoint.interpreter)(
                endpoint.wrap(input)
              )
              .flatMap(printer.printOutput)
              .as(ExitCode.Success)
              .recoverWith {
                case e if endpoint.errorable.flatMap(_.liftError(e)).nonEmpty =>
                  printer
                    .printError(e)
                    .as(ExitCode.Error)
              }
        }
      }
  }

  private def opts: Opts[IO[ExitCode]] = service.endpoints
    .foldMapK(endpointSubcommand(_))

  def command: Command[IO[ExitCode]] = {
    Command(
      toKebabCase(service.id.name),
      header = service.hints
        .get[Documentation]
        .map(_.value)
        .getOrElse(s"Command line interface for ${service.id.show}"),
      helpFlag = true
    )(opts)
  }
}
