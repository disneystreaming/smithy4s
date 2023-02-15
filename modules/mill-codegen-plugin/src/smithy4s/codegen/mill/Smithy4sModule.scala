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
import scala.util.{Success, Try}
import mill._
import mill.api.PathRef
import mill.define.Sources
import mill.scalalib._
import smithy4s.codegen.{
  CodegenArgs,
  Codegen => Smithy4s,
  FileType,
  BuildInfo,
  JarUtils,
  SMITHY4S_DEPENDENCIES
}
import mill.modules.Jvm
import mill.scalalib.CrossVersion.Binary
import mill.scalalib.CrossVersion.Constant
import mill.scalalib.CrossVersion.Full

trait Smithy4sModule extends ScalaModule {

  /** Input directory for .smithy files */
  protected def smithy4sInputDirs: Sources = T.sources {
    Seq(PathRef(millSourcePath / "smithy"))
  }

  protected def smithy4sOutputDir: T[PathRef] = T {
    PathRef(T.ctx().dest / "scala")
  }

  protected def smithy4sResourceOutputDir: T[PathRef] = T {
    PathRef(T.ctx().dest / "resources")
  }

  protected def smithy4sGeneratedSmithyMetadataFile: T[PathRef] = T {
    PathRef(T.ctx().dest / "smithy" / "generated-metadata.smithy")
  }

  protected def generateOpenApiSpecs: T[Boolean] = true

  def smithy4sAllowedNamespaces: T[Option[Set[String]]] = None

  def smithy4sExcludedNamespaces: T[Option[Set[String]]] = None

  def smithy4sDefaultIvyDeps: T[Agg[Dep]] = Agg(
    ivy"${BuildInfo.alloyOrg}:alloy-core:${BuildInfo.alloyVersion}"
  )

  def smithy4sIvyDeps: T[Agg[Dep]] = T { Agg.empty[Dep] }

  def smithy4sAllDeps: T[Agg[Dep]] = T {
    smithy4sDefaultIvyDeps() ++ smithy4sIvyDeps()
  }

  override def manifest: T[Jvm.JarManifest] = T {
    val m = super.manifest()
    val deps = smithy4sIvyDeps().iterator.toList.flatMap { d =>
      val mod = d.dep.module
      val org = mod.organization.value
      val name = mod.name.value
      val version = d.dep.version
      d.cross match {
        case Binary(_)      => List(s"$org::$name:$version")
        case Constant(_, _) => List(s"$org:$name:$version")
        case Full(_)        => Nil
      }
    }
    if (deps.nonEmpty) {
      m.add(SMITHY4S_DEPENDENCIES -> deps.mkString(","))
    } else m
  }

  def smithy4sInternalDependenciesAsJars: T[List[PathRef]] = T {
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
    smithy4sAllDeps() ++ T
      .traverse(moduleDeps) {
        case m if m.isInstanceOf[Smithy4sModule] =>
          m.asInstanceOf[Smithy4sModule].smithy4sTransitiveIvyDeps
        case _ => T.task(mill.api.Result.create(Agg.empty))
      }()
      .flatten
  }

  def smithy4sExternallyTrackedIvyDeps: T[Agg[Dep]] = T {
    resolveDeps(transitiveIvyDeps)().flatMap { pathRef =>
      val deps = JarUtils
        .extractSmithy4sDependencies(pathRef.path.toIO)
        .map(dep => ivy"$dep")
      Agg.from(deps)
    }
  }

  def smithy4sAllExternalDependencies: T[Agg[Dep]] = T {
    transitiveIvyDeps() ++
      smithy4sTransitiveIvyDeps() ++
      smithy4sExternallyTrackedIvyDeps()
  }

  def smithy4sResolvedAllExternalDependencies: T[Agg[PathRef]] = T {
    resolveDeps(smithy4sAllExternalDependencies)()
  }

  def smithy4sAllDependenciesAsJars: T[Agg[PathRef]] = T {
    smithy4sInternalDependenciesAsJars() ++
      smithy4sResolvedAllExternalDependencies()
  }

  def smithy4sWildcardArgument: T[String] = T {
    // This logic configures the default wildcard argument based on the scala version and scalac options
    // In the following scenarios we use "?" instead of "_"
    // 1. Scala version >= 3.1 ("_" is deprecated in 3.1 and becomes an error in 3.2)
    // 2. Scala version is 3 and "-source:future" or "-source future" are in scalac options
    val version = scalaVersion()
    val majorVersion = version.takeWhile(_ != '.')
    val minorVersion =
      version.drop(majorVersion.length + 1).takeWhile(_ != '.')
    def scalaOptionsContainsSourceFuture() = {
      val options = scalacOptions()
      options.contains("-source:future") || options
        .sliding(2, 1)
        .contains(Seq("-source", "future"))
    }
    (Try(majorVersion.toInt), Try(minorVersion.toInt)) match {
      case (Success(3), Success(minorVersion)) if minorVersion >= 1 => "?"
      case (Success(3), _) if scalaOptionsContainsSourceFuture()    => "?"
      case _                                                        => "_"
    }
  }

  def smithy4sGeneratedSmithyFiles: Sources = T.sources {
    val file = smithy4sGeneratedSmithyMetadataFile().path
    val wildcardArg = smithy4sWildcardArgument()
    os.remove(file)
    os.write(
      file,
      s"""$$version: "2"
         |metadata smithy4sWildcardArgument = "$wildcardArg"
         |""".stripMargin,
      createFolders = true
    )
    Seq(PathRef(file))
  }

  def smithy4sCodegen: T[(PathRef, PathRef)] = T {

    val specFiles = (smithy4sGeneratedSmithyFiles() ++ smithy4sInputDirs())
      .map(_.path)
      .filter(os.exists(_))

    val scalaOutput = smithy4sOutputDir().path
    val resourcesOutput = smithy4sResourceOutputDir().path

    val skipResources: Set[FileType] =
      if (smithy4sSmithyLibrary()) Set.empty
      else Set(FileType.Resource)

    val skipOpenApi: Set[FileType] =
      if (generateOpenApiSpecs()) Set.empty
      else Set(FileType.Openapi)

    val skipSet = skipResources ++ skipOpenApi

    val allLocalJars =
      smithy4sAllDependenciesAsJars().map(_.path).iterator.to(List)

    val args = CodegenArgs(
      specs = specFiles.toList,
      output = scalaOutput,
      resourceOutput = resourcesOutput,
      skip = skipSet,
      discoverModels = false,
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
