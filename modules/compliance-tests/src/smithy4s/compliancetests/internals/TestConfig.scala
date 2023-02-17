package smithy4s.compliancetests.internals

import smithy.test.AppliesTo
import smithy.test.AppliesTo.{CLIENT, SERVER}
import smithy4s.compliancetests.internals.TestConfig.TestType

case class TestConfig(appliesTo: AppliesTo, testType: TestType) {
  def show: String = s"(${appliesTo.name.toLowerCase}|$testType)"
}

object TestConfig {

   val clientReq = TestConfig(CLIENT, TestType.Request)
   val clientRes = TestConfig(CLIENT, TestType.Response)
   val serverReq = TestConfig(SERVER, TestType.Request)
   val serverRes = TestConfig(SERVER, TestType.Response)
  case class TestConfigs(configs: Set[TestConfig]){
    def minus(config: TestConfig): Set[TestConfig] = configs - config

    def +(config: TestConfig): TestConfigs = TestConfigs(configs + config)
    def ++(other: TestConfigs): TestConfigs = TestConfigs(configs ++ other.configs)

  }
  val client: TestConfigs = TestConfigs(Set(clientReq, clientRes))
  val server: TestConfigs = TestConfigs(Set(serverReq, serverRes))
  val all: TestConfigs = client ++ server




  sealed trait TestType

  object TestType {
    case object Request extends TestType

    case object Response extends TestType
  }
}
