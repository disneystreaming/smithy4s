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
import smithy4s.example.MapWithMemberHints
import smithy4s.example.ListWithMemberHints
import smithy.api.{Documentation => Doc}

class CollectionMemberTraitsSmokeSpec() extends FunSuite {

  test("Traits applied to map members are generated as member hints") {
    MapWithMemberHints.underlyingSchema match {
      case m: Schema.MapSchema[String, Int] =>
        val keyDoc = m.key.hints.memberHints.get(Doc)
        val valueDoc = m.value.hints.memberHints.get(Doc)
        assertEquals(keyDoc, Some(Doc("mapFoo")))
        assertEquals(valueDoc, Some(Doc("mapBar")))
        assert(m.key.hints.targetHints.isEmpty)
        assert(m.value.hints.targetHints.isEmpty)
      case _ => fail("expected map Schema")
    }
  }

  test("Traits applied to list members are generated as member hints") {
    ListWithMemberHints.underlyingSchema match {
      case Schema.CollectionSchema(_, _, _, member) =>
        val memberDoc = member.hints.memberHints.get(Doc)
        assertEquals(memberDoc, Some(Doc("listFoo")))
        assert(member.hints.targetHints.isEmpty)
      case _ => fail("expected map Schema")
    }
  }

}
