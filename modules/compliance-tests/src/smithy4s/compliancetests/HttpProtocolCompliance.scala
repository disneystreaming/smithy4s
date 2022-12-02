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
      service: Service[Alg]
  )(implicit ce: CompatEffect[F]): List[ComplianceTest[F]] =
    new internals.ClientHttpComplianceTestCase[F, Alg](
      reverseRouter,
      service
    ).allClientTests()

  def serverTests[F[_], Alg[_[_, _, _, _, _]]](
      router: Router[F],
      service: Service[Alg]
  )(implicit ce: CompatEffect[F]): List[ComplianceTest[F]] =
    new internals.ServerHttpComplianceTestCase[F, Alg](
      router,
      service
    ).allServerTests()

  def clientAndServerTests[F[_], Alg[_[_, _, _, _, _]]](
      router: Router[F] with ReverseRouter[F],
      service: Service[Alg]
  )(implicit ce: CompatEffect[F]): List[ComplianceTest[F]] =
    clientTests(router, service) ++ serverTests(router, service)

}
