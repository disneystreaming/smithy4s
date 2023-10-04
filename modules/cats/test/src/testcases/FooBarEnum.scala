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

package smithy4s.interopcats.testcases

import smithy4s.schema.Schema
import smithy4s.{Hints, ShapeId}

sealed abstract class FooBar(val stringValue: String, val intValue: Int)
    extends smithy4s.Enumeration.Value {
  override type EnumType = FooBar

  override def enumeration: smithy4s.Enumeration[EnumType] = FooBar

  val name = stringValue
  val value = stringValue
  val hints = Hints.empty

}

object FooBar
    extends smithy4s.Enumeration[FooBar]
    with smithy4s.ShapeTag.Companion[FooBar] {
  case object Foo extends FooBar("foo", 0)

  case object Bar extends FooBar("neq", 1)

  override def id: ShapeId = ShapeId("smithy4s.example", "FooBar")

  override def hints: Hints = Hints.empty

  override def values: List[FooBar] = List(Foo, Bar)

  implicit val schema: Schema[FooBar] =
    Schema.stringEnumeration[FooBar](List(Foo, Bar))
}
