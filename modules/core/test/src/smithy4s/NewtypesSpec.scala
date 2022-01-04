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

object NewtypesSpec extends weaver.FunSuite {

  type AccountId = AccountId.Type
  object AccountId extends Newtype[String] {
    def id = ShapeId("foo", "AccountId")
  }

  type DeviceId = DeviceId.Type
  object DeviceId extends Newtype[String] {
    def id = ShapeId("foo", "DeviceId")
  }

  val id1 = "id-1"
  val id2 = "id-2"

  test("Newtypes are consistent") {
    expect.all(
      AccountId(id1).value == id1,
      AccountId(id1).value != AccountId(id2).value,
      implicitly[Hints.Key[AccountId]] != implicitly[Hints.Key[DeviceId]],
      AccountId.unapply(AccountId(id1)) == Some(id1)
    )
  }

  test("Newtypes have well defined unapply") {
    val aid = AccountId(id1)
    aid match {
      case AccountId(id) => expect(id == id1)
    }
  }
}
