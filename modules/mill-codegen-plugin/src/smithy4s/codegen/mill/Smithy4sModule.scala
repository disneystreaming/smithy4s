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

package smithy4s.codegen.mill

import coursier.maven.MavenRepository
import mill._
import mill.api.PathRef
import mill.define.Source
import mill.scalalib._
import smithy4s.codegen.{CodegenArgs, Codegen => Smithy4s, FileType}
import smithy4s.codegen.mill.BuildInfo

trait Smithy4sModule extends ScalaModule {

  /** Input directory for .smithy files */
  protected def smithy4sInputDir: Source = T.source {
    PathRef(millSourcePath / "smithy")
  }

  protected def smithy4sOutputDir: T[PathRef] = T {
    PathRef(T.ctx().dest / "scala")
  }

  protected def smithy4sResourceOutputDir: T[PathRef] = T {
    PathRef(T.ctx().dest / "resources")
  }
  protected def generateOpenApiSpecs: T[Boolean] = true

  def smithy4sAllowedNamespaces: T[Option[Set[String]]] = None

  def smithy4sExcludedNamespaces: T[Option[Set[String]]] = None

  def smithy4sIvyDeps: T[Agg[Dep]] = T { Agg.empty[Dep] }

  def smithy4sLocalJars: T[List[PathRef]] = T {
    T.traverse(moduleDeps)(_.jar)
      .map(_.toList.map(_.path).map(PathRef(_)))
  }

  def smithy4sModelTransformers: T[List[String]] = List.empty[String]

  def smithy4sRepositories: T[List[String]] = repositoriesTask().toList
    .collect { case repository: MavenRepository =>
      repository.root
    }

  def smithy4sVersion: T[String] = BuildInfo.version
  def smithy4sSmithyLibrary: T[Boolean] = true

  def smithy4sTransitiveIvyDeps: T[Agg[Dep]] = T {
    smithy4sIvyDeps() ++ T
      .traverse(moduleDeps)(_.transitiveIvyDeps)()
      .flatten
  }

  def smithy4sResolvedIvyDeps: T[Agg[PathRef]] = T {
    resolveDeps(T.task { smithy4sTransitiveIvyDeps() })()
  }

  def smithy4sCodegen: T[(PathRef, PathRef)] = T {

    val specFiles = List(smithy4sInputDir().path).filter(os.exists(_))

    val scalaOutput = smithy4sOutputDir().path
    val resourcesOutput = smithy4sResourceOutputDir().path

    val skipResources: Set[FileType] =
      if (smithy4sSmithyLibrary()) Set.empty
      else Set(FileType.Resource)

    val skipOpenApi: Set[FileType] =
      if (generateOpenApiSpecs()) Set.empty
      else Set(FileType.Openapi)

    val skipSet = skipResources ++ skipOpenApi

    val resolvedDeps = smithy4sResolvedIvyDeps().iterator.map(_.path).toList

    val localJars = smithy4sLocalJars().map(_.path)
    val allLocalJars = localJars ++ resolvedDeps

    val args = CodegenArgs(
      specs = specFiles.toList,
      output = scalaOutput,
      resourceOutput = resourcesOutput,
      skip = skipSet,
      discoverModels = true,
      allowedNS = smithy4sAllowedNamespaces(),
      excludedNS = smithy4sExcludedNamespaces(),
      repositories = smithy4sRepositories(),
      dependencies = List.empty,
      transformers = smithy4sModelTransformers(),
      localJars = allLocalJars
    )
    Smithy4s.processSpecs(args)
    (PathRef(scalaOutput), PathRef(resourcesOutput))
  }

  override def generatedSources: T[Seq[PathRef]] = T {
    val (scalaOutput, _) = smithy4sCodegen()
    scalaOutput +: super.generatedSources()
  }

  def generatedResources: T[PathRef] = T {
    smithy4sCodegen()
    smithy4sResourceOutputDir()
  }

  override def localClasspath = super.localClasspath() :+ generatedResources()
}
