package smithy4s.compliancetests

import smithy4s.compliancetests.AllowRule.{HasNamespace, HasShapeId}
import smithy4s.ShapeId

sealed trait AllowRule
object AllowRule {
  final case class HasTestId(id: String) extends AllowRule
  final case class HasNamespace(ns: String) extends AllowRule
  final case class HasShapeId(id: ShapeId) extends AllowRule
}

final case class AllowRules(tests: Set[AllowRule]) {

  def ++(tests: AllowRules): AllowRules =
    this.copy(tests = this.tests ++ tests.tests)

  def isAllowed[F[_]](complianceTest: ComplianceTest[F]): Boolean =
    tests.exists {
      case AllowRule.HasTestId(id)    => complianceTest.id == id
      case AllowRule.HasNamespace(ns) => complianceTest.endpoint.namespace == ns
      case AllowRule.HasShapeId(id)   => complianceTest.endpoint == id
    }
}
object AllowRules {
  def ns(ns: String): AllowRules = AllowRules(Set(HasNamespace(ns)))
  def shapeIds(namespace: String, ids: Set[String]): AllowRules = AllowRules(
    ids.map(id => HasShapeId(ShapeId(namespace, id)))
  )

  def testIds(ids: Set[String]): AllowRules = AllowRules(
    ids.map(AllowRule.HasTestId)
  )

}
