/*
 *  Copyright 2021 Disney Streaming
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

object Smithy4sCodegenPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin

  object autoImport {
    val smithy4sCodegen =
      taskKey[Seq[File]](
        "Generate .scala and other files from smithy specs (.smithy or .json files)"
      )

    val smithy4sVersion =
      settingKey[String]("Smithy4sVersion")

    val smithy4sInputDir =
      settingKey[File](
        "Input directory for smithy specs (.smithy or .json files)"
      )

    val smithy4sOutputDir =
      settingKey[File](
        "Output directory for .scala files generated by smithy4s"
      )

    val smithy4sOpenapiDir =
      settingKey[File](
        "Output directory for openapi .json files generated by smithy4s"
      )

    val smithy4sAllowedNamespaces =
      settingKey[List[String]](
        "Allow-list of namespaces that should be processed by the generator. If unset, considers all namespaces but stdlib ones"
      )

    val smithy4sExcludedNamespaces =
      settingKey[List[String]](
        "Disallow-list of namespaces that should not be processed by the generator. When set, namespaces are evicted as the last filtering step"
      )

    @deprecated(
      "2022-03-01",
      """use `libraryDependencies += "org.acme" % "artifact" % "version" % Smithy4s`"""
    )
    val smithy4sCodegenDependencies =
      settingKey[List[String]](
        "List of dependencies containing smithy files to include in codegen task"
      )

    val Smithy4s =
      config("smithy4s").describedAs("Dependencies for Smithy code.")

    val smithy4sModelTransformers =
      settingKey[List[String]](
        "List of transformer names that should be applied to the model prior to codegen"
      )
  }

  import autoImport._

  override lazy val buildSettings = Seq(
    smithy4sVersion := BuildInfo.version
  )

  override def projectConfigurations: Seq[Configuration] = Seq(Smithy4s)

  import sbt.nio.Keys.{fileInputs, fileOutputs}

  override lazy val projectSettings =
    Seq(
      Compile / smithy4sInputDir := (Compile / sourceDirectory).value / "smithy",
      Compile / smithy4sOutputDir := (Compile / sourceManaged).value,
      Compile / smithy4sOpenapiDir := (Compile / resourceManaged).value,
      Compile / smithy4sCodegen := cachedSmithyCodegen(Compile).value,
      Compile / smithy4sCodegen / fileInputs ++= {
        Option((Compile / smithy4sInputDir).value.listFiles())
          .getOrElse(Array.empty)
          .toSeq
          .map(_.toGlob)
      },
      Compile / smithy4sCodegen / fileOutputs ++=
        Option(
          (Compile / smithy4sOutputDir).value
            .listFiles()
        )
          .getOrElse(Array.empty)
          .map(_.toGlob)
          .toSeq,
      Compile / smithy4sCodegenDependencies := List.empty: @annotation.nowarn,
      Compile / sourceGenerators +=
        (Compile / smithy4sCodegen).taskValue
          .map(_.filter(_.ext == "scala")),
      Compile / resourceGenerators +=
        (Compile / smithy4sCodegen).taskValue
          .map(_.filterNot(_.ext == "scala")),
      cleanFiles += (Compile / smithy4sOutputDir).value,
      Compile / smithy4sModelTransformers := List.empty
    )

  private def prepareSmithy4sDeps(deps: Seq[ModuleID]): List[String] =
    deps
      .filter { _.configurations.contains(Smithy4s.name) }
      .map { m =>
        if (CrossVersion.disabled == m.crossVersion)
          s"${m.organization}:${m.name}:${m.revision}"
        else s"${m.organization}::${m.name}:${m.revision}"
      }
      .toList

  /**
    * This implementation leverages SBT's input & output file tracking
    * capabilities. We record inputs via `fileInputs` and outputs
    * via `fileOutputs` and then use the `inputFileChanges` value
    * to decide whether or not Codegen should run.
    */
  def cachedSmithyCodegen(conf: Configuration) = Def.task {
    val outputPath = (conf / smithy4sOutputDir).value.getAbsolutePath()
    val openApiOutputPath = (conf / smithy4sOpenapiDir).value.getAbsolutePath()
    val allowedNamespaces =
      (conf / smithy4sAllowedNamespaces).?.value.map(_.toSet)
    val excludedNamespaces =
      (conf / smithy4sExcludedNamespaces).?.value.map(_.toSet)
    val dependencies = prepareSmithy4sDeps(libraryDependencies.value)
    val res =
      (conf / resolvers).value.toList.collect { case m: MavenRepository =>
        m.root
      }
    val transforms = (conf / smithy4sModelTransformers).value

    val out = streams.value
    val cacheFile =
      out.cacheDirectory / s"smithy4s_${scalaBinaryVersion.value}"

    // This is important - it's what re-triggers this task on file changes
    val _ = (conf / smithy4sCodegen).inputFileChanges

    val schemas = ((conf / smithy4sInputDir).value ** "*.smithy").get().toSet

    out.log.debug(s"[Smithy4s] discovered specs: $schemas")

    val compile = FileFunction
      .cached(
        cacheFile,
        inStyle = FilesInfo.lastModified,
        outStyle = FilesInfo.hash
      ) { (filePaths: Set[File]) =>
        val codegenArgs = CodegenArgs(
          filePaths.map(os.Path(_)).toList,
          output = os.Path(outputPath),
          openapiOutput = os.Path(openApiOutputPath),
          skipScala = false,
          skipOpenapi = false,
          allowedNS = allowedNamespaces,
          excludedNS = excludedNamespaces,
          repositories = res,
          dependencies = dependencies,
          transforms
        )

        val resPaths = smithy4s.codegen.Codegen
          .processSpecs(codegenArgs)
          .map(_.toIO)

        out.log.debug(s"[Smithy4s] generated files: $resPaths")

        resPaths
      }

    compile(schemas).toSeq
  }
}
