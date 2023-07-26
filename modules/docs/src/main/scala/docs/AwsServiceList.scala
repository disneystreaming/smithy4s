package docs

import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.github.plokhotnyuk.jsoniter_scala.core._
import scala.io.Source
import aws.protocols._

object AwsServiceList {

  def renderServiceList(): Unit = {
    val summary = getSummary()
    val supportedProtocols = Set(
      AwsJson1_0.schema.shapeId.name,
      AwsJson1_1.schema.shapeId.name
    )
    val (supported, unsupported) =
      summary.artifacts.partition(a => supportedProtocols(a.protocol))

    def render(emoji: String, artifactList: Vector[Artifact]): Unit = {
      artifactList.groupBy(_.protocol).foreach { case (protocol, artifacts) =>
        println(s"\n### $protocol\n")
        artifacts.foreach { artifact =>
          println(s"\n#### $emoji ${artifact.service}\n")
          val sbt =
            s""""${artifact.organization}" % "${artifact.name}" % "${summary.version}""""
          val mill =
            s"""ivy"${artifact.organization}:${artifact.name}:${summary.version}""""
          println(s"* sbt: $sbt")
          println(s"* mill: $mill")
        }
      }
    }

    println("\n### ✅ Supported (at least partially)\n")
    render("✅", supported)
    println("\n### ❌ Unsupported at this time\n")
    render("❌", unsupported)
  }

  case class Artifact(
      service: String,
      organization: String,
      name: String,
      protocol: String
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
