/*
 *  Copyright 2021-2024 Disney Streaming
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
import munit._
import software.amazon.smithy.model.shapes.ShapeId
import java.util.stream.Collectors
import software.amazon.smithy.model.Model
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
        localJars = Nil
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

  test("ModelLoader can load a dependency from s01") {
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

  test("ModelLoader can load a dependency from s01 if it has a + in the name") {
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
}
