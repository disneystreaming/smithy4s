/*
 *  Copyright 2021 Disney Streaming
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

package smithy4s.codegen.cli

import cats.data.Validated
import cats.syntax.all._
import com.monovore.decline.Command
import com.monovore.decline.Opts
import smithy4s.codegen.CodegenArgs

import Options._

object CodegenCommand {

  val outputOpt =
    Opts
      .option[os.Path](
        long = "output",
        help = "Path where scala code should be generated. Defaults to pwd",
        short = "o"
      )
      .mapValidated(path =>
        if (os.exists(path) && !os.isDir(path))
          Validated.invalidNel(s"$path is not a directory")
        else Validated.valid(path)
      )
      .orNone

  val openApiOutputOpt =
    Opts
      .option[os.Path](
        long = "openapi-output",
        help = "Path where openapi should be generated. Defaults to pwd"
      )
      .mapValidated(path =>
        if (os.exists(path) && !os.isDir(path))
          Validated.invalidNel(s"$path is not a directory")
        else Validated.valid(path)
      )
      .orNone

  val skipOpenapiOpt =
    Opts
      .flag(
        long = "skip-openapi",
        help = "Indicates that openapi specs generation should be skipped"
      )
      .orFalse

  val skipScalaOpt =
    Opts
      .flag(
        long = "skip-scala",
        help = "Indicates that scala code generation should be skipped"
      )
      .orFalse

  val allowedNSOpt: Opts[Option[Set[String]]] =
    Opts
      .option[String](
        "allowed-ns",
        "Comma-delimited list of namespaces that should not be processed. If unset, all namespaces are processed (except stdlib ones)"
      )
      .map(_.split(',').toSet)
      .orNone

  val excludedNSOpt: Opts[Option[Set[String]]] =
    Opts
      .option[String](
        "excluded-ns",
        "Comma-delimited list of namespaces that should not be processed. If unset, all namespaces are processed (except stdlib ones)"
      )
      .map(_.split(',').toSet)
      .orNone

  val options =
    (
      outputOpt,
      openApiOutputOpt,
      skipScalaOpt,
      skipOpenapiOpt,
      allowedNSOpt,
      excludedNSOpt,
      repositoriesOpt,
      dependenciesOpt,
      transformersOpt,
      specsArgs
    )
      .mapN {
        // format: off
        case (output, openApiOutput, skipScala, skipOpenapi, allowedNS, excludedNS, repositories, dependencies, transformers, specsArgs) =>
        // format: on
          CodegenArgs(
            specsArgs,
            output.getOrElse(os.pwd),
            openApiOutput.getOrElse(os.pwd),
            skipScala,
            skipOpenapi,
            allowedNS,
            excludedNS,
            repositories.getOrElse(List.empty),
            dependencies.getOrElse(List.empty),
            transformers.getOrElse(List.empty)
          )
      }

  val command = Command(
    "generate",
    "Generates scala code and openapi-specs from smithy specs"
  )(options.map(Smithy4sCommand.Generate))

}
