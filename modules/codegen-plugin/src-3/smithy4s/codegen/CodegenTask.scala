/*
 *  Copyright 2021-2026 Disney Streaming
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

import sbt.Keys._
import sbt._
import sbt.util.CacheImplicits.given
import xsbti.HashedVirtualFileRef

import JsonConverters._
import Smithy4sCodegenPlugin.autoImport._

private[codegen] object CodegenTask {
  @scala.annotation.nowarn("cat=deprecation")
  private def input(conf: Configuration) = Def.uncachedTask {
    val inputDirs =
      Option((conf / smithy4sInputDirs).value).getOrElse(Seq.empty)
    val generatedFiles = (conf / smithy4sGeneratedSmithyFiles).value
    val specs = (inputDirs ++ generatedFiles)
      .filter(_.exists())
      .toList
      .map(_.getAbsolutePath)
      .sorted
      .map(os.Path(_))
    val localJars = (conf / smithy4sAllDependenciesAsJars).value.toList.sorted
      .map(os.Path(_))
    val repositories = (conf / resolvers).value.toList.collect {
      case m: MavenRepository => m.root
    }
    val skipResources: Set[FileType] =
      if ((conf / smithy4sSmithyLibrary).value) Set.empty
      else Set(FileType.Resource)
    val skipOpenApi: Set[FileType] =
      if ((conf / smithy4sGenerateOpenApiSpecs).value) Set.empty
      else Set(FileType.Openapi)

    CodegenArgs(
      specs,
      output = os.Path((conf / smithy4sOutputDir).value),
      resourceOutput = os.Path((conf / smithy4sResourceDir).value),
      skip = skipResources ++ skipOpenApi,
      discoverModels = false,
      allowedNS = (conf / smithy4sAllowedNamespaces).?.value.map(_.toSet),
      excludedNS = (conf / smithy4sExcludedNamespaces).?.value.map(_.toSet),
      repositories = repositories,
      dependencies = List.empty,
      transformers = (conf / smithy4sModelTransformers).value,
      localJars = localJars,
      smithyBuild = (conf / smithyBuild).value.map(os.Path(_)),
      allowDefaultRepositories = (conf / smithy4sAllowDefaultRepositories).value
    )
  }

  private def cached(conf: Configuration) = Def.cachedTask {
    val args = input(conf).value
    val (root, staging) = CacheSupport.layout(conf, "codegen").value
    val paths = CacheSupport.OutputPaths(root, staging)
    val converter = fileConverter.value
    streams.value.log.debug("[smithy4s] Generating sources")
    val result = Codegen.generate(args)
    val outputs = (result.sources ++ result.resources)
      .map { entry =>
        val destination = paths.nativePath(entry.toPath.toNIO)
        CacheSupport.writeOutput(destination.toFile) { temporary =>
          val staged = entry match {
            case CodegenEntry.FromMemory(_, content) =>
              CodegenEntry.FromMemory(os.Path(temporary), content)
            case CodegenEntry.FromDisk(_, source) =>
              CodegenEntry.FromDisk(os.Path(temporary), source)
          }
          internals.CodegenImpl.write(CodegenResult(Seq(staged), Seq.empty))
        }
        destination
      }
      .distinct
      .sortBy(_.toString)
      .map(converter.toVirtualFile)
    outputs.foreach(output => Def.declareOutput(output))
    outputs.map(output => output: HashedVirtualFileRef)
  }

  def codegen(conf: Configuration) = Def.uncachedTask {
    val (root, staging) = CacheSupport.layout(conf, "codegen").value
    val converter = fileConverter.value
    val outputs = cached(conf).value
    CacheSupport.OutputPaths(root, staging).materialize(outputs, converter)
  }

  private def metadataInput(config: Configuration) = Def.uncachedTask {
    (
      (config / smithy4sWildcardArgument).value,
      (config / smithy4sGeneratedSmithyMetadataFile).value.getAbsolutePath
    )
  }

  private def cachedMetadata(config: Configuration) = Def.cachedTask {
    val (wildcardArg, destination) = metadataInput(config).value
    val (root, staging) = CacheSupport.layout(config, "metadata").value
    val converter = fileConverter.value
    val file = CacheSupport
      .OutputPaths(root, staging)
      .nativePath(new File(destination).toPath)
    streams.value.log.debug("[smithy4s] Generating metadata")
    CacheSupport.writeOutput(file.toFile) { temporary =>
      IO.write(
        temporary,
        s"""$$version: "2"
           |metadata smithy4sWildcardArgument = "$wildcardArg"
           |""".stripMargin
      )
    }
    val output = converter.toVirtualFile(file)
    Def.declareOutput(output)
    Seq(output: HashedVirtualFileRef)
  }

  def generatedSmithyFiles(config: Configuration) = Def.uncachedTask {
    val (root, staging) = CacheSupport.layout(config, "metadata").value
    val converter = fileConverter.value
    val outputs = cachedMetadata(config).value
    CacheSupport.OutputPaths(root, staging).materialize(outputs, converter)
  }
}
