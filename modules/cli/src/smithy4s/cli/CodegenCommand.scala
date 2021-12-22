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

package smithy4s.cli

import cats.data.Validated
import cats.data.ValidatedNel
import cats.syntax.all._
import com.monovore.decline.Argument
import com.monovore.decline.Command
import com.monovore.decline.Opts
import os.Path
import smithy4s.codegen.CodegenArgs

import java.nio.file

object CodegenCommand {

  implicit val osPathArg: Argument[os.Path] = new Argument[os.Path] {
    def defaultMetavar: String = "path"
    def read(string: String): ValidatedNel[String, Path] =
      implicitly[Argument[file.Path]].read(string).andThen { path =>
        try {
          if (path.isAbsolute()) Validated.validNel(os.Path(path))
          else Validated.validNel(os.pwd / os.RelPath(path))
        } catch {
          case e: Throwable =>
            Validated.invalidNel(e.getMessage() + ":" + string)
        }
      }

  }

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

  val specsArgs = Opts
    .arguments[os.Path]()
    .mapValidated(
      _.traverse(path =>
        if (os.exists(path)) Validated.valid(path)
        else Validated.invalidNel(s"$path does not exist")
      )
    )
    .orNone
    .map {
      case Some(value) => value.toList
      case None        => List.empty
    }

  val repositoriesOpt: Opts[Option[List[String]]] =
    Opts
      .option[String](
        "repositories",
        "Comma-delimited list of repositories to look in for resolving any provided dependencies"
      )
      .map(_.split(',').toList)
      .orNone

  val dependenciesOpt: Opts[Option[List[String]]] =
    Opts
      .option[String](
        "dependencies",
        "Comma-delimited list of dependencies containing smithy files"
      )
      .map(_.split(',').toList)
      .orNone

  val options =
    (
      outputOpt,
      openApiOutputOpt,
      skipScalaOpt,
      skipOpenapiOpt,
      allowedNSOpt,
      repositoriesOpt,
      dependenciesOpt,
      specsArgs
    )
      .mapN {
        // format: off
        case (output, openApiOutput, skipScala, skipOpenapi, allowedNS, repositories, dependencies, specsArgs) =>
        // format: on
          CodegenArgs(
            specsArgs,
            output.getOrElse(os.pwd),
            openApiOutput.getOrElse(os.pwd),
            skipScala,
            skipOpenapi,
            allowedNS,
            repositories.getOrElse(List.empty),
            dependencies.getOrElse(List.empty)
          )
      }

  val command = Command(
    "smithy4s-codegen",
    "Generates scala code and openapi-specs from smithy specs"
  )(options)

}
