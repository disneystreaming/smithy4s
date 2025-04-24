/*
 *  Copyright 2021-2025 Disney Streaming
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

import Smithy4sCodegenPlugin.autoImport._
import scala.collection.immutable.ListSet

private final case class SmithyBuildData(
    sources: ListSet[String],
    deps: ListSet[String],
    repos: ListSet[String]
) {
  def addAll(
      sources: ListSet[String],
      deps: ListSet[String],
      repos: ListSet[String]
  ): SmithyBuildData = {
    SmithyBuildData(
      this.sources ++ sources,
      this.deps ++ deps,
      this.repos ++ repos
    )
  }
}

private[codegen] object GenerateSmithyBuild {

  lazy val command = Command.command(
    "smithy4sUpdateLSPConfig",
    briefHelp =
      "Write a smithy-build.json file from the modules' configuration.",
    detail =
      """|Export the configuration from all modules where Smithy4sCodegenPlugin is enabled into a smithy-build.sjon
         |file. If a file already exists, the content will be merged with the existing file. If not, a new file is written.""".stripMargin
  ) { s =>
    val extracted = Project.extract(s)

    val rootDir = new File(extracted.structure.root)

    val SmithyBuildData(sources, deps, repos) = extractInfo(extracted, rootDir)

    val json = SmithyBuildJson.toJson(sources, deps, repos)
    val target = rootDir / "smithy-build.json"
    val content = if (target.exists()) {
      val content = IO.readLines(target).mkString("\n")
      SmithyBuildJson.merge(content, json)
    } else json

    IO.write(target, content)

    extracted.appendWithoutSession(Seq.empty, s)
  }

  private def extractInfo(
      extracted: Extracted,
      rootDir: File
  ): SmithyBuildData =
    extracted.structure.allProjectRefs
      .foldLeft(SmithyBuildData(ListSet.empty, ListSet.empty, ListSet.empty)) {
        case (gsb, pr) =>
          gsb.addAll(
            extractSources(pr, extracted.structure.data, rootDir),
            extractDeps(pr, extracted.structure.data),
            extractRepos(pr, extracted.structure.data)
          )
      }

  private def extractDeps(
      pr: ProjectRef,
      settings: Settings[Scope]
  ): ListSet[String] = {
    val scalaBin = (pr / scalaBinaryVersion).get(settings)

    (pr / libraryDependencies)
      .get(settings)
      .toList
      .flatten
      .filter(_.configurations.exists(_.contains(Smithy4s.name)))
      .flatMap(Smithy4sCodegenPlugin.moduleIdEncode(_, scalaBin))
      .to[ListSet]
  }

  private def extractRepos(
      pr: ProjectRef,
      settings: Settings[Scope]
  ): ListSet[String] = {
    (pr / resolvers)
      .get(settings)
      .toList
      .flatten
      .collect(prepareResolvers)
      .to[ListSet]
  }

  private def extractSources(
      pr: ProjectRef,
      settings: Settings[Scope],
      rootDir: File
  ): ListSet[String] =
    (pr / Compile / smithy4sInputDirs)
      .get(settings)
      .toList
      .flatten
      .collect(prepareInputDirs(rootDir))
      .to[ListSet]

  private val prepareResolvers: PartialFunction[Resolver, String] = {
    case mr: MavenRepository if !mr.root.contains("repo1.maven.org") => mr.root
  }

  private def prepareInputDirs(
      base: File
  ): PartialFunction[File, String] = {
    // exclude files that are under sourceManaged
    case file =>
      file.relativeTo(base) match {
        case None        => file.getAbsolutePath()
        case Some(value) => value.toString()
      }
  }
}
