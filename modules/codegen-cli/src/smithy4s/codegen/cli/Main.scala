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

import cats.data.NonEmptyList
import cats.syntax.all._
import com.monovore.decline.Command
import com.monovore.decline.Opts
import smithy4s.codegen.Codegen

object Main {

  val commands: Command[Smithy4sCommand] =
    Command("smithy4s", "Command line interface for Smithy4s")(
      NonEmptyList
        .of(
          CodegenCommand.command,
          DumpModelCommand.command,
          ProtoTypeCommand.command
        )
        .reduceMapK(Opts.subcommand(_))
    )

  def main(args: Array[String]): Unit = {
    val out = System.out
    try {
      System.setOut(System.err)
      commands
        .parse(args.toList)
        .map {
          case Smithy4sCommand.Generate(args) =>
            Codegen.processSpecs(args).foreach(out.println)

          case Smithy4sCommand.DumpModel(args) =>
            out.println(DumpModel.run(args))

          case Smithy4sCommand.ProtoType(args) =>
            Codegen.proto(args).foreach(out.println)
        }
        .leftMap { help =>
          System.err.println(help.show)
        }
        .merge
    } catch {
      case e: Throwable => e.printStackTrace(System.err)
    } finally {
      System.setErr(out)
    }
  }

}
