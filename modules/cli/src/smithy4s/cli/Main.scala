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

import cats.syntax.all._
import smithy4s.codegen.Codegen

object Main {

  def main(args: Array[String]): Unit = {
    val out = System.out
    try {
      System.setOut(System.err)
      CodegenCommand.command.parse(args.toList) match {
        case Right(codegenArgs) =>
          Codegen.processSpecs(codegenArgs).foreach(out.println)
        case Left(help) => System.err.println(help.show)
      }
    } catch {
      case e: Throwable => e.printStackTrace(System.err)
    } finally {
      System.setErr(out)
    }
  }

}
