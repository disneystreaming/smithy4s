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

package smithy4s
package internals

import smithy4s.schema.Schema

sealed trait InputOutput extends Product with Serializable {
  @inline final def widen: InputOutput = this
}

object InputOutput extends ShapeTag.Companion[InputOutput] {

  val id = ShapeId("smithy4s", "InputOutput")

  val schema: Schema[InputOutput] = {
    val inputAlt = Schema
      .constant(Input)
      .withId(ShapeId("smithy4s", "Input"))
      .oneOf[InputOutput]("input")
    val outputAlt = Schema
      .constant(Output)
      .withId(ShapeId("smithy4s", "Output"))
      .oneOf[InputOutput]("output")
    Schema
      .union(inputAlt, outputAlt) {
        case Input  => 0
        case Output => 1
      }
      .withId(id)
  }

  case object Input extends InputOutput
  case object Output extends InputOutput

}
