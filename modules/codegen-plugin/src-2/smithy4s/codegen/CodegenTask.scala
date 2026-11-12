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
import sbt.util.CacheImplicits._
import sbt.{fileJsonFormatter => _, _}

import JsonConverters._
import Smithy4sCodegenPlugin.autoImport._

private[codegen] object CodegenTask {
  @scala.annotation.nowarn("cat=deprecation")
  def codegen(conf: Configuration) = Def.task {
    val inputDirs = Option((conf / smithy4sInputDirs).value).getOrElse(Seq())
    val generatedFiles = (conf / smithy4sGeneratedSmithyFiles).value
    val inputFiles =
      (inputDirs ++ generatedFiles)
        .filter(_.exists())
        .toList
    val outputPath = (conf / smithy4sOutputDir).value
    val resourceOutputPath = (conf / smithy4sResourceDir).value
    val allowedNamespaces =
      (conf / smithy4sAllowedNamespaces).?.value.map(_.toSet)
    val excludedNamespaces =
      (conf / smithy4sExcludedNamespaces).?.value.map(_.toSet)
    val localJars =
      (conf / smithy4sAllDependenciesAsJars).value.toList.sorted
        .map(p => os.Path(p))
    val res =
      (conf / resolvers).value.toList.collect { case m: MavenRepository =>
        m.root
      }
    val allowDefaultRepos = (conf / smithy4sAllowDefaultRepositories).value
    val transforms = (conf / smithy4sModelTransformers).value
    val s = (conf / streams).value
    val skipResources: Set[FileType] =
      if ((conf / smithy4sSmithyLibrary).value) Set.empty
      else Set(FileType.Resource)

    val skipOpenApi: Set[FileType] =
      if ((conf / smithy4sGenerateOpenApiSpecs).value) Set.empty
      else Set(FileType.Openapi)

    val skipSet = skipResources ++ skipOpenApi

    val filePaths = inputFiles.map(_.getAbsolutePath())

    val specs = filePaths.sorted.map(p => os.Path(p)).toList

    val smithyBuildValue = (conf / smithyBuild).value.map(os.Path(_))

    val codegenArgs = CodegenArgs(
      specs,
      output = os.Path(outputPath),
      resourceOutput = os.Path(resourceOutputPath),
      skip = skipSet,
      discoverModels = false,
      allowedNS = allowedNamespaces,
      excludedNS = excludedNamespaces,
      repositories = res,
      dependencies = List.empty,
      transformers = transforms,
      localJars = localJars,
      smithyBuild = smithyBuildValue,
      allowDefaultRepositories = allowDefaultRepos
    )

    val cacheStoreFactory = s.cacheStoreFactory.sub(scalaVersion.value)
    val cached =
      CachedTask.inputChanged[CodegenArgs, Seq[File]](
        cacheStoreFactory.make("input"),
        s.log
      ) {
        Function.untupled {
          Tracked.lastOutput[(Boolean, CodegenArgs), Seq[File]](
            cacheStoreFactory.make("output")
          ) { case ((inputChanged, args), outputs) =>
            if (inputChanged || outputs.isEmpty) {
              s.log.debug(s"[smithy4s] Input changed: $inputChanged")
              s.log.debug(s"[smithy4s] Outputs empty: ${outputs.isEmpty}")
              s.log.debug("[smithy4s] Sources will be regenerated")
              val resPaths = smithy4s.codegen.Codegen
                .generateToDisk(args)
                .toList
              resPaths.map(path => new File(path.toString))
            } else {
              s.log.debug("[smithy4s] Using cached version of outputs")
              outputs.getOrElse(Seq.empty)
            }
          }
        }
      }

    cached(codegenArgs)
  }

  def generatedSmithyFiles(config: Configuration) = Def.task {
    val cacheFactory =
      (config / streams).value.cacheStoreFactory.sub(scalaVersion.value)
    val cached = Tracked.inputChanged[String, Seq[File]](
      cacheFactory.make("smithy4sGeneratedSmithyFilesInput")
    ) { case (changed, (wildcardArg)) =>
      val lastOutput = Tracked.lastOutput[Boolean, Seq[File]](
        cacheFactory.make("smithy4sGeneratedSmithyFilesOutput")
      ) { case (changed, prevResult) =>
        if (changed || prevResult.isEmpty) {
          val file =
            (config / smithy4sGeneratedSmithyMetadataFile).value
          IO.write(
            file,
            s"""$$version: "2"
               |metadata smithy4sWildcardArgument = "$wildcardArg"
               |""".stripMargin
          )
          Seq(file)
        } else {
          prevResult.get
        }
      }
      lastOutput(changed)
    }
    val wildcardArg = (config / smithy4sWildcardArgument).value
    cached(wildcardArg)
  }
}
