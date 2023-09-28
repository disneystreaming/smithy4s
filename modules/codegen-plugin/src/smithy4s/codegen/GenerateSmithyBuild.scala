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

import sbt._
import sbt.Keys._
import Smithy4sCodegenPlugin.autoImport._

private final case class SmithyBuildData(
    imports: Seq[String],
    deps: Seq[String],
    repos: Seq[String]
) {
  def addAll(
      imports: Seq[String],
      deps: Seq[String],
      repos: Seq[String]
  ): SmithyBuildData = {
    SmithyBuildData(
      this.imports ++ imports,
      this.deps ++ deps,
      this.repos ++ repos
    )
  }
}

private[codegen] object GenerateSmithyBuild {

  lazy val command = Command.command(
    "generateSmithyBuildJson",
    briefHelp =
      "Write a smithy-build.json file from the modules' configuration.",
    detail =
      """|Export the configuration from all modules where Smithy4sCodegenPlugin is enabled""".stripMargin
  ) { s =>
    val extracted = Project.extract(s)

    val rootDir = new File(extracted.structure.root)

    val SmithyBuildData(imports, deps, repos) = extractInfo(extracted, rootDir)

    val json = SmithyBuildJson.toJson(imports, deps, repos)
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
      .foldLeft(SmithyBuildData(Seq.empty, Seq.empty, Seq.empty)) {
        case (gsb, pr) =>
          gsb.addAll(
            extractImports(pr, extracted.structure.data, rootDir),
            extractDeps(pr, extracted.structure.data),
            extractRepos(pr, extracted.structure.data)
          )
      }

  private def extractDeps(
      pr: ProjectRef,
      settings: Settings[Scope]
  ): Seq[String] = {
    val scalaBin = (pr / scalaBinaryVersion).get(settings)

    (pr / libraryDependencies)
      .get(settings)
      .toList
      .flatten
      .filter(_.configurations.exists(_.contains(Smithy4s.name)))
      .flatMap(Smithy4sCodegenPlugin.moduleIdEncode(_, scalaBin))
  }

  private def extractRepos(
      pr: ProjectRef,
      settings: Settings[Scope]
  ): Seq[String] =
    (pr / resolvers)
      .get(settings)
      .toList
      .flatten
      .collect(prepareResolvers)

  private def extractImports(
      pr: ProjectRef,
      settings: Settings[Scope],
      rootDir: File
  ): Seq[String] =
    (pr / Compile / smithy4sInputDirs)
      .get(settings)
      .toList
      .flatten
      .collect(prepareInputDirs(rootDir))

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
