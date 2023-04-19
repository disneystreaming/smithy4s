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

package smithy4s.compliancetests
package internals

import smithy.test.AppliesTo
import smithy4s.compliancetests.internals.TestConfig.TestType
import smithy4s.schema.Schema

final case class AllowRules(allowList: Vector[AllowRule]) {
  def isAllowed[F[_]](complianceTest: ComplianceTest[F]): Boolean =
    allowList.exists { case AllowRule(testId, config) =>
      complianceTest.id == testId && config == complianceTest.config
    }

}

object AllowRules {
  implicit val schema: Schema[AllowRules] = {
    val allowListField = Schema
      .vector(AllowRule.schema)
      .required[AllowRules]("alloyRestJsonAllowList", _.allowList)
    Schema.struct(allowListField)(AllowRules(_))
  }
}

case class AllowRule(id: String, config: TestConfig)
object AllowRule {

  val schema: Schema[AllowRule] = {
    val idField = Schema.string.required[AllowRule]("id", _.id)
    val appliesToField =
      AppliesTo.schema.required[AllowRule]("appliesTo", _.config.appliesTo)
    val descriptionField =
      TestType.schema.required[AllowRule]("description", _.config.testType)
    Schema.struct(idField, appliesToField, descriptionField) {
      case (id, appliesTo, description) =>
        AllowRule(id, TestConfig(appliesTo, description))
    }
  }

}
