package smithy4s.compliancetests

import smithy4s.Service

/**
  * A construct allowing for running http protocol compliance tests against the implementation of a given protocol.
  *
  * Http protocol compliance tests are a bunch of Smithy traits provided by AWS to express expectations against
  * service definitions, making test specifications protocol-agnostic.
  *
  * See https://awslabs.github.io/smithy/2.0/additional-specs/http-protocol-compliance-tests.html?highlight=test
  */
object HttpProtocolCompliance {

  def clientTests[F[_], Alg[_[_, _, _, _, _]]](
      reverseRouter: ReverseRouter[F],
      serviceProvider: Service.Provider[Alg]
  )(implicit ce: CompatEffect[F]): List[ComplianceTest[F]] =
    new internals.ClientHttpComplianceTestCase[F, Alg](
      reverseRouter,
      serviceProvider
    ).allClientTests()

  def serverTests[F[_], Alg[_[_, _, _, _, _]]](
      router: Router[F],
      serviceProvider: Service.Provider[Alg]
  )(implicit ce: CompatEffect[F]): List[ComplianceTest[F]] =
    new internals.ServerHttpComplianceTestCase[F, Alg](
      router,
      serviceProvider
    ).allServerTests()

  def clientAndServerTests[F[_], Alg[_[_, _, _, _, _]]](
      router: Router[F] with ReverseRouter[F],
      serviceProvider: Service.Provider[Alg]
  )(implicit ce: CompatEffect[F]): List[ComplianceTest[F]] =
    clientTests(router, serviceProvider) ++ serverTests(router, serviceProvider)

}
