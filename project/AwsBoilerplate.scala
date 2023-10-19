import sbt._
import scala.annotation.tailrec
import java.net.URLClassLoader
import scala.jdk.CollectionConverters._
import scala.io.Source
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import scala.collection.immutable

object AwsBoilerplate {

  def generate(directory: java.io.File): Seq[File] = {
    val summary = loadSummary()
    val serviceValues = summary.artifacts.map {
      case Module(service, artifactName) =>
        s"""  val ${toCamelCase(service)} = "$artifactName""""
    }
    val thisYear = java.time.OffsetDateTime.now().getYear()
    val copyright =
      s"""/*
         | *  Copyright 2021-$thisYear Disney Streaming
         | *
         | *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
         | *  you may not use this file except in compliance with the License.
         | *  You may obtain a copy of the License at
         | *
         | *     https://disneystreaming.github.io/TOST-1.0.txt
         | *
         | *  Unless required by applicable law or agreed to in writing, software
         | *  distributed under the License is distributed on an "AS IS" BASIS,
         | *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
         | *  See the License for the specific language governing permissions and
         | *  limitations under the License.
         | */
         |""".stripMargin

    val content =
      s"""|$copyright
          |package smithy4s.codegen
          |
          |object AwsSpecs {
          |  val org = "${Dependencies.AwsSpecSummary.org}"
          |  val knownVersion = "${Dependencies.AwsSpecSummary.awsSpecSummaryVersion}"
          |
          |${serviceValues.mkString(System.lineSeparator())}
          |}
      """.stripMargin

    val target = directory / "smithy4s" / "codegen" / "AwsSpecs.scala"
    sbt.IO.write(target, content)
    Seq(target)
  }

  private def loadSummary(): Summary = {
    import Dependencies.AwsSpecSummary._
    val urls = coursierapi.Fetch
      .create()
      .addDependencies(
        coursierapi.Dependency.of(org, name, awsSpecSummaryVersion)
      )
      .fetch()
      .asScala
      .map(_.toURI().toURL())
      .toArray
    val cl = new URLClassLoader(urls)
    val jsonString = Source
      .fromResource("summary.json", cl)
      .getLines()
      .mkString(System.lineSeparator())

    com.github.plokhotnyuk.jsoniter_scala.core
      .readFromString[Summary](jsonString)
  }

  private case class Module(service: String, name: String)

  private case class Summary(artifacts: Vector[Module])

  private object Summary {
    implicit val jsoncodec: JsonValueCodec[Summary] = JsonCodecMaker.make

  }

  private def toCamelCase(str: String): String = {
    str.split('-').toList match {
      case head :: tl => (head :: tl.map(_.capitalize)).mkString
      case Nil        => ""
    }
  }

}
