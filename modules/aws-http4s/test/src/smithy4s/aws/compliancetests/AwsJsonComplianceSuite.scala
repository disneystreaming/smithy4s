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

package smithy4s.aws

import aws.protocols.AwsJson1_0
import aws.protocols.AwsJson1_1
import aws.protocols.RestJson1
import cats.effect.IO
import smithy4s.dynamic.DynamicSchemaIndex
import smithy4s.ShapeId
import cats.syntax.all._
import smithy4s.aws.AwsReverseRouter
import smithy4s.compliancetests._
import smithy4s.tests.ProtocolComplianceSuite

object AwsJsonComplianceSuite extends ProtocolComplianceSuite {

  // filtering out Null operation as we dont support sparse yet
  // filtering out HostWithPathOperation as this would be taken-care of by middleware.
  // filtering PutWithContentEncoding until we implement compression

  override def allRules(
      dsi: DynamicSchemaIndex
  ): IO[ComplianceTest[IO] => ShouldRun] = IO.pure {
    val disallow = Set(
      "AwsJson11MapsSerializeNullValues",
      "AwsJson11ListsSerializeNull",
      "AwsJson11MapsDeserializeNullValues",
      "AwsJson11ListsDeserializeNull",
      "HostWithPathOperation",
      "PutWithContentEncoding"
    )
    (complianceTest: ComplianceTest[IO]) =>
      if (disallow.exists(complianceTest.show.contains(_))) ShouldRun.No
      else ShouldRun.Yes
  }

  override def allTests(dsi: DynamicSchemaIndex): List[ComplianceTest[IO]] = {
    val awsJson1_0 = genClientTests(
      AwsReverseRouter.impl(AwsJson1_0),
      ShapeId("aws.protocoltests.json10", "JsonRpc10")
    )(dsi)
    val awsJson1_1 = genClientTests(
      AwsReverseRouter.impl(AwsJson1_1),
      ShapeId("aws.protocoltests.json", "JsonProtocol")
    )(dsi)
    val restJson1 = genClientTests(
      AwsReverseRouter.impl(RestJson1),
      ShapeId("aws.protocoltests.restjson", "RestJson")
    )(dsi)

    awsJson1_0 ++ awsJson1_1 ++ restJson1
  }

  private val modelDump = fileFromEnv("MODEL_DUMP")

  override def dynamicSchemaIndexLoader: IO[DynamicSchemaIndex] = {
    for {
      p <- modelDump
      dsi <- fs2.io.file
        .Files[IO]
        .readAll(p)
        .compile
        .toVector
        .map(_.toArray)
        .map(decodeDocument(_, smithy4s.http.json.codecs()))
        .flatMap(loadDynamic(_).liftTo[IO])
    } yield dsi
  }

}
