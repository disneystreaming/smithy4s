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
import smithy4s.example.collision.ReservedNameService

class ReservedTypeSmokeSpec() extends FunSuite {

  test("Idiomatic ADTs can be generated from unions") {
    val service = new ReservedNameService[Option] {
      def list(value: List[String]): Option[Unit] = None
      def map(value: Map[String, String]): Option[Unit] = None
      def option(value: Option[String]): Option[Unit] = None
      def set(set: Set[String]): Option[Unit] = None
    }
    assertEquals(service.list(List("foo")), None)
    assertEquals(service.map(Map("foo" -> "bar")), None)
    assertEquals(service.option(Some("foo")), None)
    assertEquals(service.set(Set("foo")), None)
  }

}
