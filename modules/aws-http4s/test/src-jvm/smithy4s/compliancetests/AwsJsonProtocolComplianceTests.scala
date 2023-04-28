package smithy4s.aws

import aws.protocols.AwsJson1_0
import aws.protocols.AwsJson1_1
import cats.effect.IO
import smithy4s.dynamic.DynamicSchemaIndex
import smithy4s.ShapeId
import cats.syntax.all._
import smithy4s.aws.AwsJson.impl
import smithy4s.compliancetests._
import smithy4s.compliancetests.weaverimpl._

object AwsJsonProtocolComplianceTests extends ProtocolComplianceTestSuite {

  override def allTests: DynamicSchemaIndex => List[ComplianceTest[IO]] =
    dsi => {
      genClientTests(impl(AwsJson1_0), awsJson1_0)(dsi) ++ genClientTests(
        impl(AwsJson1_1),
        awsJson1_1
      )(dsi)
    }

  private val awsJson1_0 = ShapeId("aws.protocoltests.json10", "JsonRpc10")
  private val awsJson1_1 = ShapeId("aws.protocoltests.json", "JsonProtocol")

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
