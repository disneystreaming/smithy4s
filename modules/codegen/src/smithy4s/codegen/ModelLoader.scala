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

import coursier._
import coursier.parse.DependencyParser
import coursier.parse.RepositoryParser
import software.amazon.smithy.model.Model

import java.io.File
import java.net.URL
import java.net.URLClassLoader

object ModelLoader {
  def load(
      specs: Set[File],
      dependencies: List[String],
      repositories: List[String]
  ): (ClassLoader, Model) = {
    val maybeDeps = resolveDependencies(dependencies, repositories)
    val currentClassLoader = this.getClass().getClassLoader()

    // Loads a model using whatever's on the current classpath (in particular, anything that
    // might be provided by smithy4s itself out of the box)
    val modelBuilder =
      Model
        .assembler()
        .discoverModels(currentClassLoader)
        .assemble()
        .unwrap()
        .toBuilder()

    maybeDeps.foreach { deps =>
      // Loading the model just from upstream dependencies, in isolation
      val upstreamClassLoader = new URLClassLoader(deps)
      val upstreamModel = Model
        .assembler()
        .discoverModels(upstreamClassLoader)
        .assemble()
        .unwrap()

      // Appending all shapes to the current model, so that the ones from dependencies
      // override the ones that smithy4s might have brought. This circumvents
      // collision of shapes. It does mean that what the dependencies user defines have
      // priority other what smithy4s might bring .
      modelBuilder.addShapes(upstreamModel)
    }

    val validatorClassLoader = maybeDeps match {
      case Some(deps) => new URLClassLoader(deps, currentClassLoader)
      case None       => currentClassLoader
    }

    val modelAssembler =
      Model.assembler(validatorClassLoader).addModel(modelBuilder.build())

    specs.map(_.toPath()).foreach {
      modelAssembler.addImport
    }

    val model = modelAssembler
      .assemble()
      .unwrap()

    (validatorClassLoader, model)
  }

  private def resolveDependencies(
      dependencies: List[String],
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
    if (dependencies.nonEmpty) Some {
      val fetch = Fetch().addRepositories(repos: _*).addDependencies(deps: _*)
      val files = fetch.run()
      files.map(_.toURI().toURL()).toArray
    }
    else None
  }

}
