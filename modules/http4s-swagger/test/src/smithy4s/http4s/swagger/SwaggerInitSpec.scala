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

package smithy4s.http4s.swagger

import cats.effect.IO
import weaver._
import cats.effect.Resource
import scala.io.Source
import org.webjars.WebJarAssetLocator
import cats.data.NonEmptyList

object SwaggerInitSpec extends SimpleIOSuite {
  private lazy val swaggerUiVersion =
    new WebJarAssetLocator().getWebJars.get("swagger-ui-dist")

  test("swagger-dist version compliance") {
    val inJarPath =
      s"/META-INF/resources/webjars/swagger-ui-dist/$swaggerUiVersion/swagger-initializer.js"

    val content = Resource
      .make(IO.delay(Source.fromURL(getClass.getResource(inJarPath)))) { is =>
        IO.delay(is.close())
      }
      .map(_.getLines().mkString("\n"))

    content.use { fromJar =>
      val expected = SwaggerInit
        .javascript(
          NonEmptyList(
            SwaggerInit.SwaggerUrl("https://some-url", "some-name"),
            Nil
          )
        )
        .trim()
      val actual = fromJar
        .replace(
          """url: "https://petstore.swagger.io/v2/swagger.json",""",
          """urls: [ {"url": "https://some-url", "name": "some-name"} ],"""
        )
        .trim()
      IO.pure(
        expect(
          actual == expected,
          s"The following was read from the JAR:\n$actual\nWe expected the content to be:\n$expected"
        )
      )
    }
  }
}
