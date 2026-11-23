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

  /** Replaces `destination` with a fully written temporary sibling file instead
    * of writing into it in place.
    *
    * sbt 2's disk cache can replace declared outputs with symlinks into its shared
    * content-addressed store, both after restoring a hit and after storing newly
    * generated outputs. Writing through such a link modifies the stored blob and
    * can corrupt older cache entries (A -> B -> A then restores B), see
    * https://github.com/sbt/sbt/issues/9826. Replacing the destination swaps the
    * link itself, and a failing callback leaves the previous output untouched.
    *
    * The temporary file only exists inside the callback: never declare or return
    * it, only the installed destination.
    */
  def writeOutput[A](destination: File)(write: File => A): Unit = {
    IO.writeFileAtomically(destination)(write)
    ()
  }

  def layout(config: Configuration, action: String) = Def.uncachedTask {
    val root = rootOutputDirectory.value.toAbsolutePath.normalize()
    // Isolates the staging directories of different projects.
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

  /** sbt 2 only stores declared outputs beneath rootOutputDirectory, but Smithy4s
    * destinations can be anywhere (e.g. baseDirectory / "smithy_output").
    *
    * Outputs outside the root are therefore generated into a staging tree under
    * it, and only that tree is declared to sbt. The staged path reversibly
    * encodes the destination (a URL-safe Base64 filesystem root followed by the
    * ordinary path components), so the declared references identify the final
    * files without a manifest or a directory scan.
    *
    * `materialize` maps the references back to the final destinations and copies
    * only missing or content-different files. A cache hit thereby repairs deleted
    * or damaged outputs without regenerating, and leaves up-to-date files, and
    * their modification times, alone.
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
          // The copy inherits the source's mode; sbt 2.1+ stores blobs read-only.
          temporary.setWritable(true)
        }
      }
      destination.toFile
    }
  }
}
