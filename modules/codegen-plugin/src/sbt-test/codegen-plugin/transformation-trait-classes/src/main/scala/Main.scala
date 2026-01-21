/*
 *  Copyright 2021-2026 Disney Streaming
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

object Main extends App {
  val hints = my.input.MyShape.schema.hints

  require(
    hints.has[smithy.api.Documentation],
    s"Expected to have the Documentation trait, but it was missing: $hints"
  )

  require(
    hints.get[smithy.api.Documentation].get.value == "what's up doc",
    s"Documentation trait mismatch: ${hints.get[smithy.api.Documentation].get.value}"
  )
  println("all good: " + hints.all)
}
