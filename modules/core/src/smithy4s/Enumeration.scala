/*
 *  Copyright 2021-2023 Disney Streaming
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

import smithy4s.schema.EnumValue

trait Enumeration[E <: Enumeration.Value] extends ShapeTag.Companion[E] {
  def id: ShapeId
  def hints: Hints
  def values: List[E]
  lazy val valueMap = values.map(e => e.value -> e).toMap
  lazy val intValueMap = values.map(e => e.intValue -> e).toMap
  final def fromString(s: String): Option[E] = valueMap.get(s)
  final def fromOrdinal(s: Int): Option[E] = intValueMap.get(s)
}

object Enumeration {

  abstract class Value extends Product with Serializable {
    type EnumType <: Value

    def value: String
    def name: String
    def intValue: Int
    def hints: Hints
    def enumeration: Enumeration[EnumType]
  }

  object Value {
    def toSchema[E <: Value](e: E): EnumValue[E] = {
      EnumValue(e.value, e.intValue, e, e.name, e.hints)
    }
  }

}
