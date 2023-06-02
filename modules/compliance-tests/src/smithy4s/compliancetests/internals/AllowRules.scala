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
import smithy4s.ShapeId

final case class AlloyBorrowedTests(
    simpleRestJsonBorrowedTests: Map[ShapeId, AllowRules]
)

object AlloyBorrowedTests {
  implicit val schema: Schema[AlloyBorrowedTests] = {
    val borrowedTestsField = Schema
      .map(ShapeId.schema, AllowRules.schema)
      .required[AlloyBorrowedTests](
        "alloySimpleRestJsonBorrowedTests",
        _.simpleRestJsonBorrowedTests
      )
    Schema.struct(borrowedTestsField)(AlloyBorrowedTests(_))
  }
}

final case class AllowRules(
    allowList: Vector[AllowRule],
    disallowList: Vector[AllowRule]
) {
  def shouldRun[F[_]](complianceTest: ComplianceTest[F]): ShouldRun = {
    if (disallowList.exists(_.matches(complianceTest))) ShouldRun.No
    else if (allowList.exists(_.matches(complianceTest))) ShouldRun.Yes
    else ShouldRun.NotSure
  }

}

object AllowRules {
  val empty = AllowRules(Vector.empty, Vector.empty)

  implicit val schema: Schema[AllowRules] = {
    val allowListField = Schema
      .vector(AllowRule.schema)
      .required[AllowRules]("allowList", _.allowList)
      .addHints(smithy.api.Default(smithy4s.Document.array()))

    val disallowListField = Schema
      .vector(AllowRule.schema)
      .required[AllowRules]("disallowList", _.disallowList)
      .addHints(smithy.api.Default(smithy4s.Document.array()))
    Schema.struct(allowListField, disallowListField)(AllowRules(_, _))
  }
}

case class AllowRule(
    id: String,
    appliesTo: Option[AppliesTo],
    testType: Option[TestType]
) {
  def matches[F[_]](complianceTest: ComplianceTest[F]): Boolean = {
    complianceTest.id == id &&
    appliesTo.forall(_ == complianceTest.config.appliesTo) &&
    testType.forall(_ == complianceTest.config.testType)
  }
}
object AllowRule {

  val schema: Schema[AllowRule] = {
    val idField = Schema.string.required[AllowRule]("id", _.id)
    val appliesToField =
      AppliesTo.schema.optional[AllowRule]("appliesTo", _.appliesTo)
    val testTypeField =
      TestType.schema.optional[AllowRule]("testType", _.testType)
    Schema.struct(idField, appliesToField, testTypeField) {
      case (id, appliesTo, testType) =>
        AllowRule(id, appliesTo, testType)
    }
  }

}
