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

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import java.security.MessageDigest
import java.util.Base64
import sbt._
import sbt.Keys._
import xsbti.{FileConverter, HashedVirtualFileRef}

private[codegen] object CacheSupport {

  /** sbt 2's disk cache can replace declared outputs with symlinks into its shared
    * content-addressed store (CAS), both after restoring a hit and after storing
    * newly generated outputs. Neither cachedTask nor declareOutput makes an
    * arbitrary writer copy-on-write: overwriting a restored link in place can
    * corrupt an older cache entry (A -> B -> A then restores B). See
    * https://github.com/sbt/sbt/issues/9826.
    *
    * Always write to the sibling temporary file provided by sbt-io, then replace
    * the destination, not its cached target. A failed callback preserves the old
    * output; replacement is atomic where supported, with a replacement-move
    * fallback elsewhere. The helper is available at our sbt 2.0.4 minimum:
    * https://github.com/sbt/io/blob/v1.12.2/io/src/main/scala/sbt/io/IO.scala#L464-L497
    *
    * Callers must declare/return only the installed output, never this temporary
    * file. This protects all sbt2 output kinds, including cache staging files and
    * external materialization; it does not delete stale outputs or change the
    * shared generator/sbt1 writer.
    */
  def writeOutput[A](destination: File)(write: File => A): Unit = {
    IO.writeFileAtomically(destination)(write)
    ()
  }

  def layout(config: Configuration, action: String) = Def.uncachedTask {
    val root = rootOutputDirectory.value.toAbsolutePath.normalize()
    // This digest isolates projects' staging paths; it is not an implementation
    // fingerprint and does not inspect any project/generator artifact contents.
    val project = MessageDigest
      .getInstance("SHA-256")
      .digest(
        baseDirectory.value.getAbsolutePath.getBytes(StandardCharsets.UTF_8)
      )
      .map(b => f"${b & 0xff}%02x")
      .mkString
    val staging = root
      .resolve("smithy4s-cache")
      .resolve(project)
      .resolve(config.name)
      .resolve(scalaVersion.value)
      .resolve(action)
    (root.toString, staging.toString)
  }

  /** Native ActionCache only stores declared files beneath rootOutputDirectory.
    * Custom Smithy4s destinations (e.g. baseDirectory / "smithy_output") can be
    * outside it, and must remain supported without regenerating on every call.
    *
    * External files are therefore generated into an isolated cache staging tree
    * under the native root. Only that exact inventory is declared/stored/restored
    * by sbt. The uncached File-returning facade maps it back to final destinations
    * and installs only missing/content-different files, so hits repair outputs
    * without generation or modification-time churn. No global root override or
    * legacy cache is needed, and neither stale nor unrelated files are removed.
    *
    * The staged path reversibly encodes the external absolute path: its filesystem
    * root is a URL-safe Base64 component followed by its ordinary path components.
    * Thus the returned references themselves identify final files; no inventory
    * manifest or directory scan is needed, including for mixed destinations.
    * This persistent native-cache staging is distinct from writeOutput's
    * short-lived atomic-replacement files, which must never be declared.
    */
  final case class OutputPaths(root: String, staging: String) {
    private val rootPath = Paths.get(root)
    private val stagingPath = Paths.get(staging)

    def nativePath(destination: Path): Path = {
      val path = destination.toAbsolutePath.normalize()
      if (path.startsWith(rootPath)) path
      else {
        val filesystemRoot = path.getRoot
        val encodedRoot = Base64.getUrlEncoder
          .withoutPadding()
          .encodeToString(
            filesystemRoot.toString.getBytes(StandardCharsets.UTF_8)
          )
        stagingPath
          .resolve(encodedRoot)
          .resolve(filesystemRoot.relativize(path))
      }
    }

    private def finalPath(path: Path): Path =
      if (!path.startsWith(stagingPath)) path
      else {
        val relative = stagingPath.relativize(path)
        val filesystemRoot = new String(
          Base64.getUrlDecoder.decode(relative.getName(0).toString),
          StandardCharsets.UTF_8
        )
        Paths
          .get(filesystemRoot)
          .resolve(relative.subpath(1, relative.getNameCount))
      }

    // Missing, non-regular or differently sized destinations are rejected
    // without reading; otherwise the files are compared without loading either
    // one fully into memory.
    private def sameContent(source: Path, destination: Path): Boolean =
      Files.isRegularFile(destination) &&
        Files.size(source) == Files.size(destination) &&
        Files.mismatch(source, destination) == -1L

    def materialize(
        outputs: Seq[HashedVirtualFileRef],
        converter: FileConverter
    ): Seq[File] = outputs.map { ref =>
      val source = converter.toPath(ref).toAbsolutePath.normalize()
      val destination = finalPath(source)
      if (source != destination && !sameContent(source, destination)) {
        writeOutput(destination.toFile) { temporary =>
          Files.copy(
            source,
            temporary.toPath,
            java.nio.file.StandardCopyOption.REPLACE_EXISTING
          )
        }
      }
      destination.toFile
    }
  }
}
