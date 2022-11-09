package smithy4s.compliancetests

import ComplianceTest.ComplianceResult

case class ComplianceTest[F[_]](name: String, run: F[ComplianceResult])

object ComplianceTest {
  type ComplianceResult = Either[String, Unit]
}
