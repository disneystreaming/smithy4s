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

import weaver._

object ProductSerialSmokeSpec extends FunSuite {

  test(
    "Enumeration compiles when shapes called Product or Serializable exist"
  ) {
    val product = smithy4s.example.Product
    val serial = smithy4s.example.Serializable
    val foo = smithy4s.example.FooEnum.FOO
    expect(List(product, serial, foo).forall(_ != null))
  }

}
