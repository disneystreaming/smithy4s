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

package smithy4s.http.json

import smithy4s.schema.Schema._

sealed abstract class FaceCard(_value: String, _name: String, _intValue: Int)
    extends smithy4s.Enumeration.Value {
  override type EnumType = FaceCard
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: smithy4s.Hints = smithy4s.Hints.empty
  override def enumeration: smithy4s.Enumeration[EnumType] = FaceCard
  @inline final def widen: FaceCard = this
}
object FaceCard
    extends smithy4s.Enumeration[FaceCard]
    with smithy4s.ShapeTag.Companion[FaceCard] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "FaceCard")

  val hints: smithy4s.Hints = smithy4s.Hints(
    smithy.api.Documentation("FaceCard types"),
    smithy4s.IntEnum()
  )

  case object JACK extends FaceCard("JACK", "JACK", 1)
  case object QUEEN extends FaceCard("QUEEN", "QUEEN", 2)
  case object KING extends FaceCard("KING", "KING", 3)
  case object ACE extends FaceCard("ACE", "ACE", 4)
  case object JOKER extends FaceCard("JOKER", "JOKER", 5)

  val values: List[FaceCard] = List(
    JACK,
    QUEEN,
    KING,
    ACE,
    JOKER
  )
  implicit val schema: smithy4s.Schema[FaceCard] =
    enumeration(values).withId(id).addHints(hints)
}
