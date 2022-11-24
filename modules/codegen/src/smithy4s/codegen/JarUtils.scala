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

import java.util.jar.JarFile
import java.io.File

object JarUtils {
  def extractJarManifestAttribute(path: File, key: String): Option[String] = {
    val jarFile = new JarFile(path)
    Option(jarFile.getManifest()).flatMap(m =>
      Option(m.getMainAttributes().getValue(key))
    )
  }

  def extractSmithy4sDependencies(path: File): List[String] = {
    extractJarManifestAttribute(path, SMITHY4S_DEPENDENCIES).toList.flatMap {
      str =>
        str.split(',').toList
    }
  }
}
