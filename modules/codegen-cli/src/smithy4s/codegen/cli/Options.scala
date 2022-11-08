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
import cats.data.ValidatedNel
import cats.syntax.all._
import com.monovore.decline.Argument
import com.monovore.decline.Opts

import java.nio.file

object Options {
  implicit val osPathArg: Argument[os.Path] = new Argument[os.Path] {
    def defaultMetavar: String = "path"
    def read(string: String): ValidatedNel[String, os.Path] =
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

  implicit def commaDelimitedListArg[A: Argument]: Argument[List[A]] = {
    val memberArgument = implicitly[Argument[A]]
    new Argument[List[A]] {
      def defaultMetavar: String =
        s"${memberArgument.defaultMetavar},${memberArgument.defaultMetavar},..."

      def read(string: String): ValidatedNel[String, List[A]] =
        string.split(',').toList.traverse(memberArgument.read)
    }
  }

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
      .option[List[String]](
        "repositories",
        "Comma-delimited list of repositories to look in for resolving any provided dependencies"
      )
      .orNone

  val dependenciesOpt: Opts[Option[List[String]]] =
    Opts
      .option[List[String]](
        "dependencies",
        "Comma-delimited list of dependencies containing smithy files"
      )
      .orNone

  val transformersOpt: Opts[Option[List[String]]] =
    Opts
      .option[List[String]](
        "transformers",
        "Comma-delimited list of transformer names to apply to smithy files"
      )
      .orNone

  val localJarsOpt: Opts[Option[List[os.Path]]] =
    Opts
      .option[List[os.Path]](
        "localJars",
        "Comma-delimited list of local JAR files containing smithy files"
      )
      .orNone
}
