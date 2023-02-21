package smithy4s.compliancetests

import smithy4s.compliancetests.internals.TestConfig
import io.circe._
import smithy.test.AppliesTo
import smithy4s.compliancetests.internals.TestConfig.TestType
case class AllowRule(id: String, configs: TestConfig)
object AllowRule {
  implicit val allowRuleDecoder: Decoder[AllowRule] = (c: HCursor) =>
    for {
      id <- c.downField("id").as[String]
      appliesTo <- c
        .downField("appliesTo")
        .as[String]
        .flatMap(
          AppliesTo
            .fromString(_)
            .toRight(DecodingFailure("Invalid appliesTo", c.history))
        )
      description <- c
        .downField("description")
        .as[String]
        .flatMap(
          TestType
            .fromString(_)
            .toRight(DecodingFailure("Invalid description", c.history))
        )

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
