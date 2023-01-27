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

import sjsonnew._
import BasicJsonProtocol._
import sbt.FileInfo
import sbt.HashFileInfo
import sjsonnew._
import cats.data.Validated.Invalid
import cats.data.Validated.Valid
import sbt.io.Hash

// Json codecs used by SBT's caching constructs
private[smithy4s] object JsonConverters {

  // This serialises a path by providing a hash of the content it points to.
  // Because the hash is part of the Json, this allows SBT to detect when a file
  // changes and invalidate its relevant caches, leading to a call to Smithy4s' code generator.
  implicit val pathFormat: JsonFormat[os.Path] =
    BasicJsonProtocol.projectFormat[os.Path, HashFileInfo](
      p => {
        if (os.isFile(p)) FileInfo.hash(p.toIO)
        else
          // If the path is a directory, we get the hashes of all files
          // then hash the concatenation of the hash's bytes.
          FileInfo.hash(
            p.toIO,
            Hash(
              os.walk(p)
                .map(_.toIO)
                .map(Hash(_))
                .foldLeft(Array.emptyByteArray)(_ ++ _)
            )
          )
      },
      hash => os.Path(hash.file)
    )

  implicit val fileTypeFormat: JsonFormat[FileType] =
    BasicJsonProtocol.projectFormat[FileType, String](
      ft => ft.name,
      str =>
        FileType.fromString(str) match {
          case Invalid(e) => throw new IllegalArgumentException(e.head)
          case Valid(a)   => a
        }
    )

  // format: off
  type GenTarget = List[os.Path] :*: os.Path :*: os.Path :*: Set[FileType] :*: Boolean:*: Option[Set[String]] :*: Option[Set[String]] :*: List[String] :*: List[String] :*: List[String] :*: List[os.Path] :*: LNil
  // format: on
  implicit val codegenArgsIso = LList.iso[CodegenArgs, GenTarget](
    { ca: CodegenArgs =>
      ("specs", ca.specs) :*:
        ("output", ca.output) :*:
        ("resourceOutput", ca.resourceOutput) :*:
        ("skip", ca.skip) :*:
        ("discoverModels", ca.discoverModels) :*:
        ("allowedNS", ca.allowedNS) :*:
        ("excludedNS", ca.excludedNS) :*:
        ("repositories", ca.repositories) :*:
        ("dependencies", ca.dependencies) :*:
        ("transformers", ca.transformers) :*:
        ("localJars", ca.localJars) :*:
        LNil
    },
    {
      case (_, specs) :*:
          (_, output) :*:
          (_, resourceOutput) :*:
          (_, skip) :*:
          (_, discoverModels) :*:
          (_, allowedNS) :*:
          (_, excludedNS) :*:
          (_, repositories) :*:
          (_, dependencies) :*:
          (_, transformers) :*:
          (_, localJars) :*: LNil =>
        CodegenArgs(
          specs,
          output,
          resourceOutput,
          skip,
          discoverModels,
          allowedNS,
          excludedNS,
          repositories,
          dependencies,
          transformers,
          localJars
        )
    }
  )

}
