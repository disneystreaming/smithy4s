/*
 *  Copyright 2021-2024 Disney Streaming
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

package demo

import smithy4s.example.{ SimpleValidatedString, TestValidatedNewTypes }

object Main extends App {
  try {
    val validatedStringOrError: Either[String, SimpleValidatedString] = SimpleValidatedString("test")
    println(
      validatedStringOrError match {
        case Right(value) => s"Success: ${TestValidatedNewTypes(value, "unwraped string test")}"
        case Left(error) => s"Error: $error"
      }
    )
  } catch {
    case _: java.lang.ExceptionInInitializerError =>
      println("failed")
      sys.exit(1)
  }
}
