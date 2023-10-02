package smithy4s.aws
package docs

import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.github.plokhotnyuk.jsoniter_scala.core._
import scala.io.Source

object AwsServiceList {

  def renderServiceList(): Unit = {
    val summary = getSummary()
    val supportedProtocols =
      smithy4s.aws.AwsProtocol.supportedProtocols.map(_.id.name).toSet
    val (supported, unsupported) =
      summary.artifacts.partition(a => supportedProtocols(a.protocol))

    def render(artifactList: Vector[Artifact]): Unit = {
      artifactList.groupBy(_.protocol).foreach { case (protocol, artifacts) =>
        println(s"\n### $protocol\n")
        artifacts.foreach { artifact =>
          val emoji =
            if (!supportedProtocols(artifact.protocol)) "❌"
            else if (artifact.streamingOperations.nonEmpty) "⚠️"
            else "✅"

          println(s"\n#### $emoji ${artifact.service}\n")
          val sbt =
            s""""${artifact.organization}" % "${artifact.name}" % "${summary.version}""""
          val mill =
            s"""ivy"${artifact.organization}:${artifact.name}:${summary.version}""""
          println(s"* sbt: $sbt")
          println(s"* mill: $mill")
          if (artifact.streamingOperations.nonEmpty) {
            println("")
            println(s"**Unsupported streaming operations**")
            artifact.streamingOperations.foreach(op => println(s"* $op"))
          }
        }
      }
    }

    println("\n### ✅ Supported (at least partially)\n")
    render(supported)
    if (unsupported.nonEmpty) {
      println("\n### ❌ Unsupported at this time\n")
      render(unsupported)
    }
  }

  case class Artifact(
      service: String,
      organization: String,
      name: String,
      protocol: String,
      streamingOperations: Vector[String]
  )
  case class Summary(version: String, artifacts: Vector[Artifact])

  object Summary {
    implicit val jsoncodec: JsonValueCodec[Summary] = JsonCodecMaker.make
  }

  def getSummary(): Summary = {
    val jsonString = Source
      .fromResource(
        "summary.json",
        this.getClass().getClassLoader()
      )
      .getLines()
      .mkString(System.lineSeparator())
    com.github.plokhotnyuk.jsoniter_scala.core
      .readFromString[Summary](jsonString)
  }

}
