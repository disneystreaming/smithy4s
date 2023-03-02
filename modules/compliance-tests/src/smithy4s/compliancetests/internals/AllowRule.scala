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

import cats.syntax.all._
import smithy.test.AppliesTo
import smithy4s.compliancetests.internals.TestConfig.TestType
import smithy4s.Document

case class AllowRule(id: String, configs: TestConfig)
object AllowRule {

  def allowRuleDecoder(document: Document): Option[AllowRule] = {
    document match {
      case Document.DObject(values) =>
        (values.get("id"), values.get("appliesTo"), values.get("description"))
          .flatMapN {
            case (
                  Document.DString(id),
                  Document.DString(appliesTo),
                  Document.DString(description)
                ) =>
              make(id, appliesTo, description)
            case _ => sys.error("Invalid allow rule")
          }
      case _ => sys.error("Invalid allow rule")

    }
  }

  private def make(
      id: String,
      appliesTo: String,
      description: String
  ): Option[AllowRule] = {
    for {
      appliesTo <- AppliesTo.fromString(appliesTo)
      description <- TestType.fromString(description)
    } yield {
      AllowRule(id, TestConfig(appliesTo, description))
    }
  }

  final case class AllowRules(tests: Vector[AllowRule]) {
    def isAllowed[F[_]](complianceTest: ComplianceTest[F]): Boolean =
      tests.exists { case AllowRule(testId, config) =>
        complianceTest.id == testId && config == complianceTest.config
      }

  }
}
