package smithy4s.compliancetests

import smithy4s.compliancetests.Rule.{HasNamespace, HasShapeId, HasTestId}
import smithy4s.ShapeId
import smithy4s.compliancetests.internals.TestConfig
import smithy4s.compliancetests.internals.TestConfig.TestConfigs

case class AllowRule(allowRule: Rule, configs: TestConfigs)
sealed trait Rule
object Rule {
  final case class HasTestId(id: String) extends Rule
  final case class HasNamespace(ns: String) extends Rule
  final case class HasShapeId(id: ShapeId) extends Rule
}

final case class AllowRules(tests: Set[AllowRule]) {

  def ++(tests: AllowRules): AllowRules =
    this.copy(tests = this.tests ++ tests.tests)

  def isAllowed[F[_]](complianceTest: ComplianceTest[F]): Boolean =
    tests.exists { case AllowRule(rule, configs) =>
      val res0 = rule match {
        case Rule.HasTestId(id) => complianceTest.id == id
        case HasNamespace(ns)   => complianceTest.endpoint.namespace == ns
        case HasShapeId(id)     => complianceTest.endpoint == id
      }
      res0 && configs.configs.contains(complianceTest.config)
    }
}
object AllowRules {
  def ns(ns: String): AllowRules = AllowRules(
    Set(AllowRule(HasNamespace(ns), TestConfig.all))
  )

  def testId(id: String, configs: TestConfigs): AllowRule = AllowRule(HasTestId(id), configs)
    def testId(id: String): AllowRule = AllowRule(HasTestId(id), TestConfig.all)

  def testIds(ids: Set[String]): AllowRules = AllowRules(
    ids.map(id => AllowRule(HasTestId(id), TestConfig.all))
  )

}
