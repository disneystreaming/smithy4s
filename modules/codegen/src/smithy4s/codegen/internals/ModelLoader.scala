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
package internals

import coursier._
import coursier.parse.DependencyParser
import coursier.parse.RepositoryParser
import software.amazon.smithy.build.ProjectionTransformer
import software.amazon.smithy.build.TransformContext
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.loader.ModelAssembler

import java.io.File
import java.net.URLClassLoader

import scala.jdk.CollectionConverters._
import software.amazon.smithy.model.loader.ModelDiscovery
import software.amazon.smithy.model.loader.ModelManifestException

private[codegen] object ModelLoader {

  def load(
      specs: Set[File],
      dependencies: List[String],
      repositories: List[String],
      transformers: List[String],
      discoverModels: Boolean,
      localJars: List[os.Path]
  ): (ClassLoader, Model) = {
    val currentClassLoader = this.getClass().getClassLoader()
    val deps = resolveDependencies(dependencies, localJars, repositories)

    val modelsInJars = deps.flatMap { files =>
      val manifestUrl =
        ModelDiscovery.createSmithyJarManifestUrl(files.getAbsolutePath())
      try { ModelDiscovery.findModels(manifestUrl).asScala }
      catch {
        case _: ModelManifestException => Seq.empty
      }
    }

    // Loading the upstream model
    val upstreamModel = Model
      .assembler()
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
      case (k @ "smithy4sGenerated", _) => ()
      case (k, _) if k.startsWith("smithy4s") =>
        sanitisingModelBuilder.removeMetadataProperty(k)
      case _ => ()
    }

    val validatorClassLoader = locally {
      val jarUrls = deps.map(_.toURI().toURL()).toArray
      new URLClassLoader(jarUrls, currentClassLoader)
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

    val trans = transformers.flatMap { t =>
      val result = serviceFactory(t)
      if (result.isPresent()) Some(result.get) else None
    }

    val transformedModel = trans.foldLeft(preTransformationModel)((m, t) =>
      t.transform(TransformContext.builder().model(m).build())
    )

    (validatorClassLoader, transformedModel)
  }

  private def resolveDependencies(
      dependencies: List[String],
      localJars: List[os.Path],
      repositories: List[String]
  ): Seq[File] = {
    val maybeRepos = RepositoryParser.repositories(repositories).either
    val maybeDeps = DependencyParser
      .dependencies(
        dependencies,
        defaultScalaVersion = smithy4s.codegen.BuildInfo.scalaBinaryVersion
      )
      .either
    val repos = maybeRepos match {
      case Left(errorMessages) =>
        throw new IllegalArgumentException(
          s"Failed to parse repositories with error: $errorMessages"
        )
      case Right(r) => r
    }
    val deps = maybeDeps match {
      case Left(errorMessages) =>
        throw new IllegalArgumentException(
          s"Failed to parse dependencies with errors: $errorMessages"
        )
      case Right(d) => d
    }
    val resolvedDeps: Seq[java.io.File] =
      if (deps.nonEmpty) {
        val fetch = Fetch().addRepositories(repos: _*).addDependencies(deps: _*)
        fetch.run()
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
      val smithy4sResources = List(
        "META-INF/smithy/smithy4s.meta.smithy"
      ).map(classLoader.getResource)

      if (discoverModels) {
        assembler.discoverModels(classLoader)
      } else addImports(smithy4sResources)
    }
  }

}
