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

import sbt._
import sbt.io.Hash
import java.net.URLClassLoader
import java.net.URL

object SwaggerCheck {
  def sha1(f: File): String = {
    Hash.toHex(Hash(f))
  }

  def sha1(jar: URL, inJarPath: String): String = {
    val usingCl: sbt.io.Using[URL, URLClassLoader] =
      sbt.io.Using.resource(
        url => new URLClassLoader(Array(url)),
        cl => cl.close()
      )

    usingCl(jar) { (cl: URLClassLoader) =>
      Hash.toHex(Hash(cl.getResourceAsStream(inJarPath)))
    }
  }

  def findJar(update: UpdateReport, artifactName: String): Option[URL] = {
    val swaggerDistJars = for {
      compileReport <- update.configurations
        .find(_.configuration.name == Compile.name)
        .toList
      module <- compileReport.modules
      artifactFile <- module.artifacts.filter(_._1.name == artifactName)
    } yield {
      val (_, file) = artifactFile
      file.toURI.toURL
    }

    swaggerDistJars.headOption
  }
}
