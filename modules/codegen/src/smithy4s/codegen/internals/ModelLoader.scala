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
package internals

import coursier._
import coursier.cache.FileCache
import coursier.parse.DependencyParser
import coursier.parse.RepositoryParser
import software.amazon.smithy.build.ProjectionTransformer
import software.amazon.smithy.build.TransformContext
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.loader.ModelAssembler

import java.io.File
import java.net.URLClassLoader

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
    val deps = resolveDependencies(
      dependencies :+ protocolDependency,
      localJars,
      repositories
    )

    val modelCL = locally {
      val jarUrls = deps.map(_.toURI().toURL()).toArray
      new URLClassLoader(jarUrls, currentClassLoader)
    }

    val preTransformationModel = Model
      .assembler(modelCL)
      // disabling cache to support snapshot-driven experimentation
      .putProperty(ModelAssembler.DISABLE_JAR_CACHE, true)
      .discoverModels(modelCL)
      .addImports(specs)
      .assemble()
      .unwrap()

    val transformerFactory =
      ProjectionTransformer.createServiceFactory(modelCL)

    val trans = transformers.flatMap {
      transformerFactory(_).asScala
    }

    val transformedModel = trans.foldLeft(preTransformationModel)((m, t) =>
      t.transform(TransformContext.builder().model(m).build())
    )

    val postTransformationModel = Model
      .assembler(modelCL)
      .addModel(transformedModel)
      .assemble()
      .unwrap

    (
      modelCL,
      postTransformationModel
    )
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
        val fetch = Fetch(FileCache())
          .addRepositories(repos: _*)
          .addDependencies(deps: _*)
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
      if (discoverModels) {
        assembler.discoverModels(classLoader)
      } else assembler
    }
  }

}
