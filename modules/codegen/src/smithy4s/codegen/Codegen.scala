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

package smithy4s.codegen

import smithy4s.openapi.OpenApiConversionResult
import software.amazon.smithy.model.Model

import scala.jdk.CollectionConverters._

object Codegen { self =>

  def processSpecs(
      args: CodegenArgs
  ): Set[os.Path] = {

    val (classloader, model) = ModelLoader.load(
      args.specs.map(_.toIO).toSet,
      args.dependencies,
      args.repositories,
      args.transformers,
      args.discoverModels
    )

    val scalaFiles = if (!args.skipScala) {
      Codegen.generate(model, args.allowedNS, args.excludedNS).map {
        case (relPath, name, outputString) =>
          val fileName = name + ".scala"
          val scalaFile = (args.output / relPath / fileName)
          os.write.over(scalaFile, outputString, createFolders = true)
          scalaFile
      }
    } else List.empty

    val openApiFiles = if (!args.skipOpenapi) {
      smithy4s.openapi.convert(model, args.allowedNS, classloader).map {
        case OpenApiConversionResult(_, serviceId, outputString) =>
          val name = serviceId.getNamespace() + "." + serviceId.getName()
          val openapiFile = (args.openapiOutput / (name + ".json"))
          os.write.over(openapiFile, outputString, createFolders = true)
          openapiFile
      }
    } else List.empty

    (scalaFiles ++ openApiFiles).toSet
  }

  private def generate(
      model: Model,
      allowedNS: Option[Set[String]],
      excludedNS: Option[Set[String]]
  ): List[(os.RelPath, String, String)] = {
    val namespaces = model
      .shapes()
      .iterator()
      .asScala
      .map(_.getId().getNamespace())
      .toSet

    val reserved =
      Set(
        "smithy4s.api",
        "smithy4s.meta"
      )
    val excluded = excludedNS.getOrElse(Set.empty)

    val filteredNamespaces = allowedNS match {
      case Some(allowedNamespaces) =>
        namespaces
          .filter(allowedNamespaces)
          .filterNot(excluded)
      case None =>
        namespaces
          .filterNot(_.startsWith("aws."))
          .filterNot(_.startsWith("smithy."))
          .filterNot(reserved)
          .filterNot(excluded)
    }

    filteredNamespaces.toList
      .map { ns => SmithyToIR(model, ns) }
      .flatMap { cu =>
        val amended = CollisionAvoidance(cu)
        Renderer(amended)
      }
      .map { result =>
        val relPath =
          os.RelPath(result.namespace.split('.').toIndexedSeq, ups = 0)
        (relPath, result.name, result.content)
      }
  }

}
