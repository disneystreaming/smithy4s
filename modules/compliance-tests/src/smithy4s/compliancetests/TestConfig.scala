/*
 *  Copyright 2021-2024 Disney Streaming
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

package smithy4s.compliancetests

import smithy.test.AppliesTo
import smithy4s.compliancetests.TestConfig.TestType
import smithy4s.Hints
import smithy4s.Enumeration
import smithy4s.ShapeId
import smithy4s.Schema

case class TestConfig(
    appliesTo: AppliesTo,
    testType: TestType
) {
  def show: String = s"(${appliesTo.name.toLowerCase}|$testType)"
}

object TestConfig {

  val clientReq = TestConfig(AppliesTo.CLIENT, TestType.Request)
  val clientRes = TestConfig(AppliesTo.CLIENT, TestType.Response)
  val serverReq = TestConfig(AppliesTo.SERVER, TestType.Request)
  val serverRes = TestConfig(AppliesTo.SERVER, TestType.Response)
  sealed abstract class TestType(
      val value: String,
      val intValue: Int
  ) extends Enumeration.Value {
    type EnumType = TestType
    def name: String = value
    def hints: Hints = Hints.empty
    def enumeration: Enumeration[EnumType] = TestType
  }
  object TestType extends smithy4s.Enumeration[TestType] {

    def id: ShapeId = ShapeId("smithy4s.compliancetests.internals", "TestType")
    def hints: Hints = Hints.empty
    def values: List[TestType] = List(Request, Response)
    case object Request extends TestType("request", 0)
    case object Response extends TestType("response", 0)

    val schema: Schema[TestType] = Schema.stringEnumeration(values)

  }
}
