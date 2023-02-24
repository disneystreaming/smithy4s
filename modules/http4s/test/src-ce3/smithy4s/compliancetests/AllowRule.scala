package smithy4s.compliancetests

import smithy4s.compliancetests.internals.TestConfig
import smithy.test.AppliesTo
import smithy4s.compliancetests.internals.TestConfig.TestType
import smithy4s.Document
import cats.syntax.all._
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
