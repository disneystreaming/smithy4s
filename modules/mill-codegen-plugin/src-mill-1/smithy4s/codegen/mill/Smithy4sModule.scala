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

package smithy4s.codegen.mill

import coursier.maven.MavenRepository
import mill._
import mill.api.PathRef
import mill.api.Task
import mill.scalalib._
import mill.util.JarManifest
import smithy4s.codegen.BuildInfo
import smithy4s.codegen.CodegenArgs
import smithy4s.codegen.FileType
import smithy4s.codegen.JarUtils
import smithy4s.codegen.SMITHY4S_DEPENDENCIES
import smithy4s.codegen.{Codegen => Smithy4s}

import scala.annotation.nowarn
import scala.util.Success
import scala.util.Try

trait Smithy4sModule extends ScalaModule {

  val AWS = smithy4s.codegen.AwsSpecs

  /** Input directory for .smithy files */
  def smithy4sInputDirs: T[Seq[PathRef]] = Task.Sources("smithy")

  /** Returns the directory containing generated Scala sources (delegates to smithy4sCodegen). */
  def smithy4sOutputDir: T[PathRef] = Task {
    smithy4sCodegen()._1
  }

  /** Returns the directory containing generated resources (delegates to smithy4sCodegen). */
  def smithy4sResourceOutputDir: T[PathRef] = Task {
    smithy4sCodegen()._2
  }

  def smithy4sGeneratedSmithyMetadataFile: T[PathRef] = Task {
    smithy4sGeneratedSmithyFiles().head
  }

  def generateOpenApiSpecs: T[Boolean] = true

  /** In Mill 1.x, tasks can only read files tracked by Task.Source/Task.Sources/Task.Input. */
  def smithyBuild: T[Option[PathRef]] = Task.Input { None }

  @deprecated(
    "Use the `smithy4sCodegen` Smithy metadata key with `allowedNamespaces` instead. See the Package Remapping documentation.",
    "0.19.5"
  )
  def smithy4sAllowedNamespaces: T[Option[Set[String]]] = None

  @deprecated(
    "Use the `smithy4sCodegen` Smithy metadata key with `excludedNamespaces` instead. See the Package Remapping documentation.",
    "0.19.5"
  )
  def smithy4sExcludedNamespaces: T[Option[Set[String]]] = None

  def smithy4sDefaultIvyDeps: T[Seq[Dep]] = Task {
    Seq(mvn"${BuildInfo.alloyOrg}:alloy-core:${BuildInfo.alloyVersion}")
  }

  def smithy4sIvyDeps: T[Seq[Dep]] = Task { Seq.empty[Dep] }

  def smithy4sAllDeps: T[Seq[Dep]] = Task {
    smithy4sDefaultIvyDeps() ++ smithy4sIvyDeps()
  }

  override def manifest: T[JarManifest] = Task {
    val m = super.manifest()
    val deps = smithy4sIvyDeps().toList.flatMap {
      Smithy4sModule.depIdEncode
    }
    if (deps.nonEmpty) {
      m.add(SMITHY4S_DEPENDENCIES -> deps.mkString(","))
    } else m
  }

  def smithy4sInternalDependenciesAsJars: T[List[PathRef]] = Task {
    Task.traverse(moduleDeps)(_.jar)().toList.map(_.path).map(PathRef(_))
  }

  def smithy4sModelTransformers: T[List[String]] = List.empty[String]

  def smithy4sRepositories: T[List[String]] = Task {
    repositoriesTask().toList
      .collect { case repository: MavenRepository =>
        repository.root
      }
  }

  /** Whether coursier's default repositories (e.g. Maven Central, ivy2Local) should
   *  be queried when resolving codegen dependencies, in addition to those declared
   *  via Mill's `repositoriesTask`. Defaults to `true` for backwards compatibility.
   */
  def smithy4sAllowDefaultRepositories: T[Boolean] = Task { true }

  def smithy4sVersion: T[String] = BuildInfo.version
  def smithy4sSmithyLibrary: T[Boolean] = true

  def smithy4sTransitiveIvyDeps: T[Seq[Dep]] = Task {
    smithy4sAllDeps() ++
      Task
        .traverse(moduleDeps) {
          case m: Smithy4sModule => m.smithy4sTransitiveIvyDeps
          case _                 => Task.Anon { Seq.empty[Dep] }
        }()
        .flatten
  }

  def smithy4sExternallyTrackedIvyDeps: T[Seq[Dep]] = Task {
    defaultResolver()
      .classpath(allMvnDeps())
      .flatMap { pathRef =>
        JarUtils
          .extractSmithy4sDependencies(pathRef.path.toIO)
          .map(dep => mvn"$dep")
      }
  }

  def smithy4sAwsSpecsVersion: T[String] = Task {
    AWS.bomVersion
  }

  def smithy4sAwsSpecs: T[Seq[String]] = Task {
    Seq.empty[String]
  }

  def smithy4sAwsSpecEntries: T[Seq[(String, String)]] = Task {
    Seq.empty[(String, String)]
  }

  def smithy4sAwsSpecDependencies: T[Seq[Dep]] = Task {
    val org = AWS.org
    val version = smithy4sAwsSpecsVersion()
    val fromEntries = smithy4sAwsSpecEntries().map { case (name, v) =>
      mvn"$org:$name:$v"
    }
    val fromLegacy = smithy4sAwsSpecs().map { name => mvn"$org:$name:$version" }
    fromEntries ++ fromLegacy
  }

  def smithy4sResolvedAllExternalDependencies: T[Seq[PathRef]] = Task {
    val bind = bindDependency()
    val smithyBound: Seq[BoundDep] =
      (smithy4sTransitiveIvyDeps() ++
        smithy4sExternallyTrackedIvyDeps() ++
        smithy4sAwsSpecDependencies()).map(bind)
    val moduleJars: Seq[PathRef] = defaultResolver().classpath(allMvnDeps())
    val smithyJars: Seq[PathRef] = defaultResolver().classpath(smithyBound)
    (moduleJars ++ smithyJars).distinct
  }

  def smithy4sAllDependenciesAsJars: T[Seq[PathRef]] = Task {
    smithy4sInternalDependenciesAsJars() ++
      smithy4sResolvedAllExternalDependencies()
  }

  def smithy4sWildcardArgument: T[String] = Task {
    val version = scalaVersion()
    val majorVersion = version.takeWhile(_ != '.')
    val minorVersion =
      version.drop(majorVersion.length + 1).takeWhile(_ != '.')
    val patchVersion =
      version.drop(majorVersion.length + minorVersion.length + 2)

    val options = scalacOptions()

    def scalaOptionsContainsSourceFuture() = {
      options.contains("-source:future") || options
        .sliding(2, 1)
        .contains(Seq("-source", "future"))
    }
    def scalaOptionsContainsKindProjectorPlaceholders() =
      options.contains("-P:kind-projector:underscore-placeholders")

    (
      Try(majorVersion.toInt),
      Try(minorVersion.toInt),
      Try(patchVersion.toInt)
    ) match {
      case (Success(3), Success(minorVersion), _) if minorVersion >= 1 => "?"
      case (Success(3), _, _) if scalaOptionsContainsSourceFuture()    => "?"
      case (Success(2), Success(13), Success(patchVersion))
          if patchVersion >= 5 && scalaOptionsContainsKindProjectorPlaceholders() =>
        "?"
      case (Success(2), Success(12), Success(patchVersion))
          if patchVersion >= 14 && scalaOptionsContainsKindProjectorPlaceholders() =>
        "?"
      case _ => "_"
    }
  }

  def smithy4sGeneratedSmithyFiles: T[Seq[PathRef]] = Task {
    val file = Task.dest / "smithy" / "generated-metadata.smithy"
    val wildcardArg = smithy4sWildcardArgument()
    os.write.over(
      file,
      s"""$$version: "2"
         |metadata smithy4sWildcardArgument = "$wildcardArg"
         |""".stripMargin,
      createFolders = true
    )
    Seq(PathRef(file))
  }

  @nowarn("cat=deprecation")
  def smithy4sCodegen: T[(PathRef, PathRef)] = Task {

    val specFiles = (smithy4sGeneratedSmithyFiles() ++ smithy4sInputDirs())
      .map(_.path)
      .filter(os.exists(_))
      .toList

    // Mill 1.x requires tasks to only write within their own Task.dest
    val scalaOutput = Task.dest / "scala"
    val resourcesOutput = Task.dest / "resources"

    val skipResources: Set[FileType] =
      if (smithy4sSmithyLibrary()) Set.empty
      else Set(FileType.Resource)

    val skipOpenApi: Set[FileType] =
      if (generateOpenApiSpecs()) Set.empty
      else Set(FileType.Openapi)

    val skipSet = skipResources ++ skipOpenApi

    val smithyBuildFile = smithyBuild().map(_.path)

    val allLocalJars =
      smithy4sAllDependenciesAsJars()
        .map(_.path)
        .iterator
        .to(List)

    val args = CodegenArgs(
      specs = specFiles,
      output = scalaOutput,
      resourceOutput = resourcesOutput,
      skip = skipSet,
      discoverModels = false,
      allowedNS = smithy4sAllowedNamespaces(),
      excludedNS = smithy4sExcludedNamespaces(),
      repositories = smithy4sRepositories(),
      dependencies = List.empty,
      transformers = smithy4sModelTransformers(),
      localJars = allLocalJars,
      smithyBuild = smithyBuildFile,
      allowDefaultRepositories = smithy4sAllowDefaultRepositories()
    )

    Smithy4s.generateToDisk(args)
    (PathRef(scalaOutput), PathRef(resourcesOutput))
  }

  override def generatedSources: T[Seq[PathRef]] = Task {
    smithy4sOutputDir() +: super.generatedSources()
  }

  def generatedResources: T[PathRef] = Task {
    smithy4sCodegen()._2
  }

  override def localClasspath: T[Seq[PathRef]] = Task {
    super.localClasspath() :+ generatedResources()
  }
}

object Smithy4sModule {
  def depIdEncode(dep: Dep): Option[String] = {
    val mod = dep.dep.module
    val org = mod.organization.value
    val name = mod.name.value
    val version = dep.dep.versionConstraint.asString
    dep.cross match {
      case mill.api.CrossVersion.Binary(_)      => Some(s"$org::$name:$version")
      case mill.api.CrossVersion.Constant(_, _) => Some(s"$org:$name:$version")
      case mill.api.CrossVersion.Full(_)        => None
    }
  }
}
