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

package smithy4s.codegen.internals
import coursierapi.MavenRepository
import coursierapi.ScalaVersion
import munit._
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ShapeId

import java.io.File
import java.util.stream.Collectors
import scala.jdk.CollectionConverters._

class ModelLoaderSpec extends FunSuite {
  private def doLoad(dependencies: List[String], repositories: List[String]) =
    ModelLoader
      .load(
        specs = Set.empty,
        dependencies,
        repositories,
        transformers = Nil,
        discoverModels = false,
        localJars = Nil,
        allowDefaultRepositories = true
      )
      ._2

  private def allNamespaces(model: Model): Set[String] =
    model
      .shapes()
      .collect(Collectors.toList())
      .asScala
      .toList
      .map(_.getId().getNamespace())
      .toSet

  test("ModelLoader can load a dependency from s01".ignore) {
    val model = doLoad(
      dependencies =
        List("org.polyvariant:test-library-core_2.13:0.0.1-SNAPSHOT"),
      repositories =
        List("https://s01.oss.sonatype.org/content/repositories/snapshots")
    )

    assertEquals(
      allNamespaces(model),
      Set("smithy.api", "smithy4s.meta", "testlibrary")
    )

    model.expectShape(ShapeId.from("testlibrary#MyString"))
  }

  test(
    "ModelLoader can load a version of the smithy4s protocol conflicting against the current"
  ) {
    doLoad(
      dependencies =
        List("com.disneystreaming.smithy4s:smithy4s-protocol:0.18.29"),
      repositories = Nil
    )
    // nothing failed
  }

  test(
    "ModelLoader can load a version of Alloy conflicting against the current"
  ) {
    doLoad(
      dependencies = List("com.disneystreaming.alloy:alloy-core:0.1.18"),
      repositories = Nil
    )
    // nothing failed
  }

  test(
    "ModelLoader can load a dependency from s01 if it has a + in the name".ignore
  ) {
    val model = doLoad(
      dependencies =
        List("org.polyvariant:test-library-core_2.13:0.0.1+123-SNAPSHOT"),
      repositories =
        List("https://s01.oss.sonatype.org/content/repositories/snapshots")
    )

    assertEquals(
      allNamespaces(model),
      Set("smithy.api", "smithy4s.meta", "testlibrary")
    )

    model.expectShape(ShapeId.from("testlibrary#MyString"))
  }

  test(
    "dropParentProvidedJars removes jars for modules already on the parent classloader"
  ) {
    val parentModules = List(
      "software.amazon.smithy:smithy-build",
      "software.amazon.smithy:smithy-model",
      "com.disneystreaming.alloy:alloy-core"
    )
    val jars = List(
      // Coursier cache (Maven layout)
      new File(
        "/root/.cache/coursier/v1/https/repo1.maven.org/maven2/software/amazon/smithy/smithy-build/1.72.0/smithy-build-1.72.0.jar"
      ),
      new File(
        "/root/.cache/coursier/v1/https/repo1.maven.org/maven2/software/amazon/smithy/smithy-model/1.72.0/smithy-model-1.72.0.jar"
      ),
      // Ivy local (dotted org, no version in filename)
      new File(
        "/root/.ivy2/local/com.disneystreaming.alloy/alloy-core/0.3.40/jars/alloy-core.jar"
      ),
      // A jar unique to the child (not on the parent) must be kept
      new File(
        "/root/.cache/coursier/v1/https/repo1.maven.org/maven2/com/example/my-shapes/1.0.0/my-shapes-1.0.0.jar"
      )
    )

    val kept = ModelLoader.dropParentProvidedJars(jars, parentModules)

    assertEquals(kept.map(_.getName), List("my-shapes-1.0.0.jar"))
  }

  test(
    "dropParentProvidedJars does not confuse a module with a same-prefixed module"
  ) {
    val parentModules = List("software.amazon.smithy:smithy-build")
    val jars = List(
      new File(
        "/cache/software/amazon/smithy/smithy-build/1.72.0/smithy-build-1.72.0.jar"
      ),
      new File(
        "/cache/software/amazon/smithy/smithy-build-tools/1.72.0/smithy-build-tools-1.72.0.jar"
      )
    )

    val kept = ModelLoader.dropParentProvidedJars(jars, parentModules)

    assertEquals(kept.map(_.getName), List("smithy-build-tools-1.72.0.jar"))
  }

  test(
    "dropParentProvidedJars keeps everything when the parent list is empty"
  ) {
    val jars = List(
      new File(
        "/cache/software/amazon/smithy/smithy-build/1.72.0/smithy-build-1.72.0.jar"
      )
    )
    assertEquals(ModelLoader.dropParentProvidedJars(jars, Nil), jars)
  }

  test(
    "ModelLoader keeps a resolved conflicting smithy-build off the child classloader"
  ) {
    // Regression test for the IllegalAccessError that arose when a dependency
    // dragged a newer smithy-build (containing IncludeClosures, whose superclass
    // BackwardCompatHelper is package-private) onto the child URLClassLoader while
    // the parent classloader held a different smithy-build version.
    assert(
      smithy4s.codegen.BuildInfo.codegenDependencies
        .contains("software.amazon.smithy:smithy-build"),
      "test precondition: smithy-build must be a parent-provided module"
    )

    val (classLoader, _) = ModelLoader.load(
      specs = Set.empty,
      dependencies = List("software.amazon.smithy:smithy-build:1.72.0"),
      repositories = Nil,
      transformers = Nil,
      discoverModels = false,
      localJars = Nil,
      allowDefaultRepositories = true
    )

    val urls = classLoader match {
      case u: java.net.URLClassLoader => u.getURLs.toList.map(_.toString)
      case _                          => Nil
    }
    assert(
      !urls.exists(_.contains("/software/amazon/smithy/smithy-build/")),
      s"smithy-build should be filtered off the child classloader, got: $urls"
    )
  }

  test("parseDependencies rejects a malformed dependency string") {
    val ex = intercept[IllegalArgumentException] {
      ModelLoader.parseDependencies(
        List("not-a-valid-dependency-string"),
        ScalaVersion.of(smithy4s.codegen.BuildInfo.scalaBinaryVersion)
      )
    }
    assert(ex.getMessage.contains("not-a-valid-dependency-string"))
  }

  test("buildFetch keeps coursier's default repositories when allowed") {
    val repos = List(MavenRepository.of("https://internal.example.com/repo"))
    val fetch = ModelLoader.buildFetch(
      dependencies = Nil,
      repositories = repos,
      allowDefaultRepositories = true
    )
    val bases = fetch.getRepositories().asScala.collect {
      case m: MavenRepository => m.getBase()
    }
    assert(bases.contains("https://repo1.maven.org/maven2"))
    assert(bases.contains("https://internal.example.com/repo"))
  }

  test("buildFetch excludes coursier's default repositories when disallowed") {
    val repos = List(MavenRepository.of("https://internal.example.com/repo"))
    val fetch = ModelLoader.buildFetch(
      dependencies = Nil,
      repositories = repos,
      allowDefaultRepositories = false
    )
    assertEquals(fetch.getRepositories().asScala.toList, repos)
  }
}
