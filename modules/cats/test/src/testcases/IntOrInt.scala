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

package smithy4s.interopcats.testcases

import smithy4s.schema.Schema._
import smithy4s.schema.Schema
import smithy4s.ShapeId

sealed trait IntOrInt
object IntOrInt {
  case class IntValue0(value: Int) extends IntOrInt
  case class IntValue1(value: Int) extends IntOrInt

  val schema: Schema[IntOrInt] = {
    val intValue0 = int.oneOf[IntOrInt]("intValue0", IntValue0(_)) {
      case IntValue0(int) => int
    }
    val intValue1 = int.oneOf[IntOrInt]("intValue1", IntValue1(_)) {
      case IntValue1(int) => int
    }
    union(intValue0, intValue1).reflective
  }.withId(ShapeId("", "IntOrInt"))

}
