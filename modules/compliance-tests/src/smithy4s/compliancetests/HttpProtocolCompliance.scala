package smithy4s.compliancetests

import smithy4s.Service
import cats.effect.IO

/**
  * A construct allowing for running http protocol compliance tests against the implementation of a given protocol.
  *
  * Http protocol compliance tests are a bunch of Smithy traits provided by AWS to express expectations against
  * service definitions, making test specifications protocol-agnostic.
  *
  * See https://awslabs.github.io/smithy/2.0/additional-specs/http-protocol-compliance-tests.html?highlight=test
  */
object HttpProtocolCompliance {

  def clientTests[Alg[_[_, _, _, _, _]]](
      reverseRouter: ReverseRouter[IO],
      serviceProvider: Service.Provider[Alg]
  )(implicit ce: CompatEffect[IO]): List[ComplianceTest[IO]] =
    new internals.ClientHttpComplianceTestCase[Alg](
      reverseRouter,
      serviceProvider
    ).allClientTests()

  def serverTests[Alg[_[_, _, _, _, _]]](
      router: Router[IO],
      serviceProvider: Service.Provider[Alg]
  )(implicit ce: CompatEffect[IO]): List[ComplianceTest[IO]] =
    new internals.ServerHttpComplianceTestCase[Alg](
      router,
      serviceProvider
    ).allServerTests()

  def clientAndServerTests[Alg[_[_, _, _, _, _]]](
      router: Router[IO] with ReverseRouter[IO],
      serviceProvider: Service.Provider[Alg]
  )(implicit ce: CompatEffect[IO]): List[ComplianceTest[IO]] =
    clientTests(router, serviceProvider) ++ serverTests(router, serviceProvider)

}
