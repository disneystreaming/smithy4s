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

import munit._
import smithy4s.schema.EnumTag

final class IntEnumSmokeSpec extends FunSuite {

  test(
    "Int enum is generated as is expected"
  ) {
    val values = smithy4s.example.FaceCard.values
    val expected = List(
      smithy4s.example.FaceCard.JACK,
      smithy4s.example.FaceCard.QUEEN,
      smithy4s.example.FaceCard.KING,
      smithy4s.example.FaceCard.ACE,
      smithy4s.example.FaceCard.JOKER
    )
    assertEquals(values, expected)
    smithy4s.example.FaceCard.schema match {
      case s: smithy4s.schema.Schema.EnumerationSchema[_] =>
        assert(s.tag.isInstanceOf[EnumTag.IntEnum[_]], "tag should be IntEnum")
      case _ => fail("schema should be an enumeration schema")
    }

  }

}
