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

package smithy4s.codegen.cli

import cats.data.Validated
import cats.syntax.all._
import com.monovore.decline.Command
import com.monovore.decline.Opts
import smithy4s.codegen.CodegenArgs

import Options._
import smithy4s.codegen.FileType

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

  val resourceOutputOpt =
    Opts
      .option[os.Path](
        long = "resource-output",
        help = "Path where non-scala files should be generated. Defaults to pwd"
      )
      .mapValidated(path =>
        if (os.exists(path) && !os.isDir(path))
          Validated.invalidNel(s"$path is not a directory")
        else Validated.valid(path)
      )
      .orNone

  val skipOpts =
    Opts
      .options[String](
        long = "skip",
        help =
          "Indicates that some files typesshould be skipped during generation"
      )
      .mapValidated(_.traverse(FileType.fromString))
      .map(_.toList.toSet)
      .orNone
      .map(_.getOrElse(Set.empty))

  val discoverModelsOpt =
    Opts
      .flag(
        long = "discover-models",
        help =
          "Indicates whether the model assembler should try to discover models in the classpath"
      )
      .orFalse

  val allowedNSOpt: Opts[Option[Set[String]]] =
    Opts
      .option[List[String]](
        "allowed-ns",
        "Comma-delimited list of namespaces that should not be processed. If unset, all namespaces are processed (except stdlib ones)"
      )
      .map(_.toSet)
      .orNone

  val excludedNSOpt: Opts[Option[Set[String]]] =
    Opts
      .option[List[String]](
        "excluded-ns",
        "Comma-delimited list of namespaces that should not be processed. If unset, all namespaces are processed (except stdlib ones)"
      )
      .map(_.toSet)
      .orNone

  val options =
    (
      outputOpt,
      resourceOutputOpt,
      skipOpts,
      discoverModelsOpt,
      allowedNSOpt,
      excludedNSOpt,
      repositoriesOpt,
      dependenciesOpt,
      transformersOpt,
      localJarsOpt,
      specsArgs
    )
      .mapN {
        // format: off
        case (output, openApiOutput, skip, discoverModels, allowedNS, excludedNS, repositories, dependencies, transformers, localJars, specsArgs) =>
        // format: on
          CodegenArgs(
            specsArgs,
            output.getOrElse(os.pwd),
            openApiOutput.getOrElse(os.pwd),
            skip,
            discoverModels,
            allowedNS,
            excludedNS,
            repositories.getOrElse(List.empty),
            dependencies.getOrElse(List.empty),
            transformers.getOrElse(List.empty),
            localJars.getOrElse(List.empty)
          )
      }

  val command = Command(
    "generate",
    "Generates scala code and openapi-specs from smithy specs"
  )(options.map(Smithy4sCommand.Generate))

}
