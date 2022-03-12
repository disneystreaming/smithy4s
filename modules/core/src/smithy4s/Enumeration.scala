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

package smithy4s

import smithy4s.schema.EnumValue

trait Enumeration[E] extends ShapeTag.Companion[E] {
  def values: List[E]
  def schemaValues: List[schema.EnumValue[E]]
  def to(e: E): schema.EnumValue[E]
  lazy val valueMap = values.map(e => to(e).name -> e).toMap
  lazy val ordinalMap = values.map(e => to(e).ordinal -> e).toMap
  final def fromString(s: String): Option[E] = valueMap.get(s)
  final def fromOrdinal(s: Int): Option[E] = ordinalMap.get(s)
}

object Enumeration {

  trait Value[E] extends Product with Serializable { self: E =>
    def name: String
    def ordinal: Int
    def hints: Hints
    def toSchemaValue: schema.EnumValue[E] =
      EnumValue(name, ordinal, self, hints)
  }

}
