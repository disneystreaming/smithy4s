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
package internals

import coursierapi.Dependency
import coursierapi.Fetch
import coursierapi.MavenRepository
import coursierapi.ScalaVersion
import software.amazon.smithy.build.ProjectionTransformer
import software.amazon.smithy.build.TransformContext
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.loader.ModelAssembler
import software.amazon.smithy.model.loader.ModelDiscovery
import software.amazon.smithy.model.loader.ModelManifestException

import java.io.File
import java.net.URLClassLoader
import java.nio.file.FileSystems
import java.nio.file.Files
import scala.jdk.CollectionConverters._
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.Using

private[codegen] object ModelLoader {

  def load(
      specs: Set[File],
      dependencies: List[String],
      repositories: List[String],
      transformers: List[String],
      discoverModels: Boolean,
      localJars: List[os.Path],
      allowDefaultRepositories: Boolean
  ): (ClassLoader, Model) = {
    val currentClassLoader = this.getClass().getClassLoader()
    val deps = resolveDependencies(
      dependencies :+ protocolDependency,
      localJars,
      repositories,
      allowDefaultRepositories
    )

    val modelsInJars = deps.flatMap { file =>
      Using.resource(
        // Note: On JDK13+, the second parameter is redundant.
        FileSystems.newFileSystem(file.toPath(), null: ClassLoader)
      ) { jarFS =>
        val p = jarFS.getPath("META-INF", "smithy", "manifest")

        // model discovery would throw if we tried to pass a non-existent path
        if (!Files.exists(p)) Nil
        else {
          try ModelDiscovery.findModels(p.toUri().toURL()).asScala.toList
          catch {
            case e: ModelManifestException =>
              System.err.println(
                s"Unexpected exception while loading model from $file, skipping: $e"
              )
              Nil
          }
        }
      }
    }

    val validatorClassLoader = locally {
      val jarUrls = deps.map(_.toURI().toURL()).toArray
      new URLClassLoader(jarUrls, currentClassLoader)
    }

    // Loading the upstream model
    val upstreamModel = Model
      .assembler(validatorClassLoader)
      // disabling cache to support snapshot-driven experimentation
      .putProperty(ModelAssembler.DISABLE_JAR_CACHE, true)
      .addClasspathModels(currentClassLoader, discoverModels)
      .addImports(modelsInJars)
      .assemble()
      .unwrap()

    val sanitisingModelBuilder = upstreamModel.toBuilder()

    // Appending all metadata that is not Smithy4s-specific, as well as relevant
    // Smithy4s-related metadata, into the resulting model.
    upstreamModel.getMetadata().asScala.foreach {
      case (CodegenRecord.METADATA_KEY, _)          => ()
      case ("smithy4sBincompatPreludeAdditions", _) => ()
      case (k, _) if k.startsWith("smithy4s") =>
        sanitisingModelBuilder.removeMetadataProperty(k)
      case _ => ()
    }

    val preTransformationModel =
      Model
        .assembler(validatorClassLoader)
        .addModel(sanitisingModelBuilder.build())
        .addImports(specs)
        .assemble()
        .unwrap

    val serviceFactory =
      ProjectionTransformer.createServiceFactory(validatorClassLoader)

    val resolvedTransformers = transformers.flatMap { t =>
      val result = serviceFactory(t)
      if (result.isPresent()) Some(result.get)
      else {
        System.err.println(s"[smithy4s] Warning: unresolved transformer: $t")
        None
      }
    }

    val transformedModel =
      resolvedTransformers.foldLeft(preTransformationModel)((m, t) =>
        t.transform(TransformContext.builder().model(m).build())
      )

    val postTransformationModel = Model
      .assembler(validatorClassLoader)
      .addModel(transformedModel)
      .assemble()
      .unwrap

    (validatorClassLoader, postTransformationModel)
  }

  private[internals] def parseDependencies(
      dependencies: List[String],
      scalaVersion: ScalaVersion
  ): List[Dependency] = {
    val (errors, deps) = dependencies.foldLeft(
      (List.empty[String], List.empty[Dependency])
    ) { case ((errors, acc), depString) =>
      Try(Dependency.parse(depString, scalaVersion)) match {
        case Success(dep) => (errors, acc :+ dep)
        case Failure(e)   => (errors :+ s"$depString: ${e.getMessage}", acc)
      }
    }
    if (errors.nonEmpty) {
      throw new IllegalArgumentException(
        s"Failed to parse dependencies with errors: $errors"
      )
    }
    deps
  }

  // Builds the Fetch request without running it, so the repository/dependency
  // wiring can be unit-tested without performing any network resolution.
  private[internals] def buildFetch(
      dependencies: List[Dependency],
      repositories: List[MavenRepository],
      allowDefaultRepositories: Boolean
  ): Fetch = {
    val baseFetch = Fetch.create()
    val withRepos =
      if (allowDefaultRepositories) baseFetch.addRepositories(repositories: _*)
      else baseFetch.withRepositories(repositories: _*)
    withRepos.addDependencies(dependencies: _*)
  }

  private def resolveDependencies(
      dependencies: List[String],
      localJars: List[os.Path],
      repositories: List[String],
      allowDefaultRepositories: Boolean
  ): Seq[File] = {
    val scalaVersion =
      ScalaVersion.of(smithy4s.codegen.BuildInfo.scalaBinaryVersion)

    val deps = parseDependencies(dependencies, scalaVersion)
    val repos = repositories.map(MavenRepository.of)

    val resolvedDeps: Seq[java.io.File] =
      if (deps.nonEmpty) {
        buildFetch(deps, repos, allowDefaultRepositories).fetch().asScala.toSeq
      } else {
        Seq.empty
      }
    resolvedDeps ++ localJars.map(_.toIO)
  }

  implicit class ModelAssemblerOps(assembler: ModelAssembler) {
    def addImports(files: Set[java.io.File]): ModelAssembler = {
      files.map(_.toPath()).foreach(assembler.addImport)
      assembler
    }

    def addImports(urls: Seq[java.net.URL]): ModelAssembler = {
      urls.foreach(assembler.addImport)
      assembler
    }

    def addClasspathModels(
        classLoader: ClassLoader,
        discoverModels: Boolean
    ): ModelAssembler = {
      if (discoverModels) {
        assembler.discoverModels(classLoader)
      } else assembler
    }
  }

}
