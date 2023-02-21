package smithy4s.compliancetests.internals

import smithy.test.AppliesTo
import smithy4s.compliancetests.internals.TestConfig.TestType

case class TestConfig(appliesTo: AppliesTo, testType: TestType) {
  def show: String = s"(${appliesTo.name.toLowerCase}|$testType)"
}

object TestConfig {

  val clientReq = TestConfig(AppliesTo.CLIENT, TestType.Request)
  val clientRes = TestConfig(AppliesTo.CLIENT, TestType.Response)
  val serverReq = TestConfig(AppliesTo.SERVER, TestType.Request)
  val serverRes = TestConfig(AppliesTo.SERVER, TestType.Response)
  sealed trait TestType

  object TestType {
    case object Request extends TestType
    case object Response extends TestType

    def fromString(s: String): Option[TestType] = s.toLowerCase match {
      case "request"  => Some(Request)
      case "response" => Some(Response)
      case _          => None
    }
  }
}
