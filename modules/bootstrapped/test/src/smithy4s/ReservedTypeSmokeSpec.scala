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

package smithy4s

import munit._
import smithy4s.example.collision.{String => SString, _}

class ReservedTypeSmokeSpec() extends FunSuite {

  test(
    "Names from the Scala stdlib can be used in smithy spec without hurting UX"
  ) {
    val service = new ReservedNameService[Option] {
      def list(value: List[SString]): Option[Unit] = None
      def map(value: Map[SString, SString]): Option[Unit] = None
      def option(value: Option[SString]): Option[Unit] = None
      def set(set: Set[SString]): Option[Unit] = None
    }
    assertEquals(service.list(List(SString("foo"))), None)
    assertEquals(service.map(Map(SString("foo") -> SString("bar"))), None)
    assertEquals(service.option(Some(SString("foo"))), None)
    assertEquals(service.set(Set(SString("foo"))), None)
  }

}
