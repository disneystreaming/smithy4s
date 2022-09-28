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

import coursier._
import coursier.parse.DependencyParser
import coursier.parse.RepositoryParser
import software.amazon.smithy.build.ProjectionTransformer
import software.amazon.smithy.build.TransformContext
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.loader.ModelAssembler

import java.io.File
import java.net.URL
import java.net.URLClassLoader

import scala.jdk.CollectionConverters._

object ModelLoader {

  def load(
      specs: Set[File],
      dependencies: List[String],
      repositories: List[String],
      transformers: List[String],
      discoverModels: Boolean,
      localJars: List[os.Path]
  ): (ClassLoader, Model) = {
    val allDeps = dependencies
    val maybeDeps = resolveDependencies(allDeps, localJars, repositories)
    val currentClassLoader = this.getClass().getClassLoader()

    // Loads a model using whatever's on the current classpath (in particular, anything that
    // might be provided by smithy4s itself out of the box)
    val modelBuilder = Model
      .assembler()
      .addClasspathModels(currentClassLoader, discoverModels)
      .assemble()
      .unwrap()
      .toBuilder()

    maybeDeps.foreach { deps =>
      // Loading the model just from upstream dependencies, in isolation
      val upstreamClassLoader = new URLClassLoader(deps)
      val upstreamModel = Model
        .assembler()
        .discoverModels(upstreamClassLoader)
        .addClasspathModels(currentClassLoader, false)
        // disabling cache to support snapshot-driven experimentation
        .putProperty(ModelAssembler.DISABLE_JAR_CACHE, true)
        .assemble()
        .unwrap()

      // Appending all shapes to the current model, so that the ones from dependencies
      // override the ones that smithy4s might have brought. This circumvents
      // collision of shapes. It does mean that what the dependencies user defines have
      // priority other what smithy4s might bring .
      modelBuilder.addShapes(upstreamModel)

      // Appending all metadata that is not Smithy4s-specific, as well as relevant
      // Smithy4s-related metadata, into the resulting model.
      upstreamModel.getMetadata().asScala.foreach {
        case (k @ "smithy4sGenerated", v) =>
          modelBuilder.putMetadataProperty(k, v)
        case (k, _) if k.startsWith("smithy4s") =>
        // do nothing, we do not want upstream decisions on smithy4s rendering to impact
        // this codegen run
        case (k, v) =>
          modelBuilder.putMetadataProperty(k, v)
      }
    }

    val validatorClassLoader = maybeDeps match {
      case Some(deps) => new URLClassLoader(deps, currentClassLoader)
      case None       => currentClassLoader
    }

    val modelAssembler =
      Model
        .assembler(validatorClassLoader)
        .addModel(modelBuilder.build())

    specs.map(_.toPath()).foreach {
      modelAssembler.addImport
    }

    val model = modelAssembler
      .assemble()
      .unwrap()

    val serviceFactory =
      ProjectionTransformer.createServiceFactory(validatorClassLoader)

    val trans = transformers.flatMap { t =>
      val result = serviceFactory(t)
      if (result.isPresent()) Some(result.get) else None
    }

    val transformedModel = trans.foldLeft(model)((m, t) =>
      t.transform(TransformContext.builder().model(m).build())
    )

    (validatorClassLoader, transformedModel)
  }

  private def resolveDependencies(
      dependencies: List[String],
      localJars: List[os.Path],
      repositories: List[String]
  ): Option[Array[URL]] = {
    val maybeRepos = RepositoryParser.repositories(repositories).either
    val maybeDeps = DependencyParser
      .dependencies(
        dependencies,
        defaultScalaVersion = BuildInfo.scalaBinaryVersion
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
    val allDeps = resolvedDeps ++ localJars.map(_.toIO)
    if (allDeps.nonEmpty) {
      Some(allDeps.map(_.toURI().toURL()).toArray)
    } else {
      None
    }
  }

  implicit class ModelAssemblerOps(assembler: ModelAssembler) {
    def addImports(urls: List[java.net.URL]): ModelAssembler = {
      urls.foreach(assembler.addImport)
      assembler
    }

    def addClasspathModels(
        classLoader: ClassLoader,
        discoverModels: Boolean
    ): ModelAssembler = {
      val smithy4sResources = List(
        "META-INF/smithy/smithy4s.smithy",
        "META-INF/smithy/smithy4s.meta.smithy"
      ).map(classLoader.getResource)

      if (discoverModels) assembler.discoverModels(classLoader)
      else addImports(smithy4sResources)
    }
  }

}
