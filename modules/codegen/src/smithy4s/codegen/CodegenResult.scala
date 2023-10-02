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

sealed trait CodegenEntry {
  def toPath: os.Path = this match {
    case CodegenEntry.FromMemory(path, _) => path
    case CodegenEntry.FromDisk(path, _)   => path
  }
}

object CodegenEntry {
  case class FromMemory(targetPath: os.Path, content: String)
      extends CodegenEntry
  case class FromDisk(targetPath: os.Path, sourcePath: os.Path)
      extends CodegenEntry
}

final case class CodegenResult(
    sources: Seq[CodegenEntry],
    resources: Seq[CodegenEntry]
)
