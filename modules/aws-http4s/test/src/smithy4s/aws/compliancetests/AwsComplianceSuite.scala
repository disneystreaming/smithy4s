/*
 *  Copyright 2021-2024 Disney Streaming
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

import aws.protocols._
import cats.effect.IO
import cats.syntax.all._
import smithy4s.ShapeId
import smithy4s.aws.AwsJson.impl
import smithy4s.compliancetests._
import smithy4s.dynamic.DynamicSchemaIndex
import smithy4s.tests.ProtocolComplianceSuite

object AwsComplianceSuite extends ProtocolComplianceSuite {

  override def allRules(
      dsi: DynamicSchemaIndex
  ): IO[ComplianceTest[IO] => ShouldRun] = IO.pure {

    // Filtering these rule because on JS-specific issues,
    // in particular around floating-point precision.
    val jsDisallowed = Set(
      "RestJsonHttpRequestLabelEscaping",
      "RestJsonInputAndOutputWithNumericHeaders",
      "RestJsonInputWithHeadersAndAllParams",
      "InputWithHeadersAndAllParams",
      "HttpRequestLabelEscaping",
      "InputAndOutputWithNumericHeaders"
    )

    val disallowed = Set(
      // This would be taken-care of by middleware.
      "HostWithPathOperation",
      // We expect users to set idempotency tokens because doing so raises
      // awareness of the need to do so outside of any retry mechanism.
      "QueryIdempotencyTokenAutoFill",

      // TODO https://github.com/disneystreaming/smithy4s/issues/1424
      "AwsJson10DeserializeIgnoreType",
      "AwsJson10ClientPopulatesDefaultValuesInInput",
      "AwsJson10ClientPopulatesDefaultsValuesWhenMissingInResponse",
      "AwsJson10ClientPopulatesNestedDefaultValuesWhenMissing",
      "AwsJson10ClientPopulatesNestedDefaultsWhenMissingInResponseBody",
      "AwsJson10ClientErrorCorrectsWhenServerFailsToSerializeRequiredValues",
      "AwsJson11DeserializeIgnoreType",
      "RestJsonHttpPayloadWithUnsetUnion",
      "RestJsonDeserializeIgnoreType",
      "RestXmlHttpPayloadWithUnsetUnion"
    )
    (complianceTest: ComplianceTest[IO]) =>
      if (disallowed.exists(complianceTest.show.contains(_))) ShouldRun.No
      else if (jsDisallowed.contains(complianceTest.id) && weaver.Platform.isJS)
        ShouldRun.No
      else ShouldRun.Yes
  }

  override def allTests(dsi: DynamicSchemaIndex): List[ComplianceTest[IO]] =
    genClientTests(impl(Ec2Query), awsEc2Query)(dsi) ++
      genClientTests(impl(AwsJson1_0), awsJson1_0)(dsi) ++
      genClientTests(impl(AwsJson1_1), awsJson1_1)(dsi) ++
      genClientTests(impl(AwsQuery), awsQuery)(dsi) ++
      genClientTests(impl(RestJson1), restJson1)(dsi) ++
      genClientTests(impl(RestXml), restXml)(dsi)

  private val awsEc2Query = ShapeId("aws.protocoltests.ec2", "AwsEc2")
  private val awsJson1_0 = ShapeId("aws.protocoltests.json10", "JsonRpc10")
  private val awsJson1_1 = ShapeId("aws.protocoltests.json", "JsonProtocol")
  private val awsQuery = ShapeId("aws.protocoltests.query", "AwsQuery")
  private val restJson1 = ShapeId("aws.protocoltests.restjson", "RestJson")
  private val restXml = ShapeId("aws.protocoltests.restxml", "RestXml")

  private val modelDump = fileFromEnv("MODEL_DUMP")

  val jsonDecoders =
    smithy4s.json.Json.payloadCodecs.withJsoniterCodecCompiler {
      smithy4s.json.Json.jsoniter.withMapOrderPreservation(true)
    }.decoders

  override def dynamicSchemaIndexLoader: IO[DynamicSchemaIndex] = {
    for {
      p <- modelDump
      dsi <- fs2.io.file
        .Files[IO]
        .readAll(p)
        .compile
        .toVector
        .map(_.toArray)
        .map(decodeDocument(_, jsonDecoders))
        .flatMap(loadDynamic(_).liftTo[IO])
    } yield dsi
  }

}
