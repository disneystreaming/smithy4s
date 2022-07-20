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
import os.Path
import smithy4s.codegen.ProtoTypeArgs
import smithy4s.codegen.cli.Smithy4sCommand.ProtoType

object ProtoTypeCommand {

  import Options._

  val smithyString =
    Opts.option[String](long = "input", help = "Smithy string ")
  val outputOpt: Opts[Option[Path]] =
    Opts
      .option[os.Path](
        long = "output",
        help = "Path where scala code should be written to. Defaults to pwd",
        short = "o"
      )
      .mapValidated(path =>
        if (os.exists(path) && !os.isDir(path))
          Validated.invalidNel(s"$path is not a directory")
        else Validated.valid(path)
      )
      .orNone

  val options: Opts[ProtoTypeArgs] =
    (smithyString, outputOpt)
      .mapN {
          // format: off
          case (smithy, outputDir) =>
            // format: on
          ProtoTypeArgs(
            smithy,
            outputDir.getOrElse(os.pwd)
          )
      }

  val command: Command[ProtoType] =
    Command(
      "proto",
      "generate code from a command line input for the purposes of learning and testing smithy4s functionality"
    )(
      options.map(ProtoType.apply)
    )

}
