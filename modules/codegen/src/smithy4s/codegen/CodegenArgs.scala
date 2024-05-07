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

package smithy4s.codegen

import cats.data.ValidatedNel
import cats.syntax.all._

final case class CodegenArgs(
    specs: List[os.Path],
    output: os.Path,
    resourceOutput: os.Path,
    skip: Set[FileType],
    discoverModels: Boolean,
    allowedNS: Option[Set[String]],
    excludedNS: Option[Set[String]],
    repositories: List[String],
    dependencies: List[String],
    transformers: List[String],
    localJars: List[os.Path],
    smithyBuild: Option[os.Path]
) {
  def skipScala: Boolean = skip(FileType.Scala)
  def skipOpenapi: Boolean = skip(FileType.Openapi)
  def skipResources: Boolean = skip(FileType.Resource)
  def skipProto: Boolean = skip(FileType.Proto)
}

sealed abstract class FileType(val name: String)
    extends scala.Product
    with Serializable

object FileType {

  def fromString(s: String): ValidatedNel[String, FileType] = values
    .find(_.name == s)
    .toValidNel(s"Expected one of ${values.map(_.name).mkString(", ")}")

  case object Scala extends FileType("scala")
  case object Openapi extends FileType("openapi")
  case object Resource extends FileType("resource")
  case object Proto extends FileType("proto")

  val values = List(Scala, Openapi, Resource)
}
