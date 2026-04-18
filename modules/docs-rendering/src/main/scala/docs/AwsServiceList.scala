package smithy4s.aws
package docs

import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.github.plokhotnyuk.jsoniter_scala.core._
import scala.io.Source

object AwsServiceList {

  def renderServiceList(): Unit = {
    val metadata = getMetadata()
    val supportedProtocols =
      smithy4s.aws.AwsProtocol.supportedProtocols.map(_.id.name).toSet
    val (supported, unsupported) =
      metadata.services.partition(s => supportedProtocols(s.protocol))

    def render(services: Vector[ServiceEntry]): Unit = {
      services.groupBy(_.protocol).foreach { case (protocol, entries) =>
        println(s"\n### $protocol\n")
        entries.foreach { entry =>
          val emoji =
            if (!supportedProtocols(entry.protocol)) "❌"
            else "✅"

          println(s"\n#### $emoji ${entry.name}\n")
          val org = smithy4s.codegen.AwsSpecs.org
          val sbt =
            s"""`"$org" % "${entry.name}" % "${entry.version}"`"""
          val mill =
            s"""`ivy"$org:${entry.name}:${entry.version}"`"""
          println(s"* sbt: $sbt")
          println(s"* mill: $mill")
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

  case class ServiceEntry(name: String, version: String, protocol: String)

  case class Metadata(services: Vector[ServiceEntry])

  object Metadata {
    implicit val jsonCodec: JsonValueCodec[Metadata] = JsonCodecMaker.make
  }

  def getMetadata(): Metadata = {
    val jsonString = Source
      .fromResource(
        "aws-service-metadata.json",
        this.getClass().getClassLoader()
      )
      .getLines()
      .mkString(System.lineSeparator())
    readFromString[Metadata](jsonString)
  }

}
