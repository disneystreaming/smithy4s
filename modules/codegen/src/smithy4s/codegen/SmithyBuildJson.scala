/*
 *  Copyright 2021-2025 Disney Streaming
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

import io.circe.Json
import io.circe.parser
import smithy4s.codegen.internals.SmithyBuild
import smithy4s.codegen.internals.SmithyBuildMaven
import smithy4s.codegen.internals.SmithyBuildMavenRepository

import scala.collection.immutable.ListSet

private[codegen] object SmithyBuildJson {

  val protocolDependency =
    s"${BuildInfo.smithy4sOrg}:${BuildInfo.protocolArtifactName}:${BuildInfo.version}"

  def toJson(
      sources: ListSet[String],
      dependencies: ListSet[String],
      repositories: ListSet[String]
  ): String = {
    SmithyBuild.writeJson(
      SmithyBuild.Serializable(
        version = "1.0",
        sources,
        SmithyBuildMaven(
          dependencies + protocolDependency,
          repositories.map(SmithyBuildMavenRepository.apply)
        )
      )
    )
  }

  def merge(
      json1: String,
      json2: String
  ): String = (for {
    j1 <- parser.parse(json1)
    j2 <- parser.parse(json2)
    merged = mergeJs(j1, j2)
  } yield merged).left.map(err => throw err).merge.spaces4

  private def mergeJs(
      v1: Json,
      v2: Json
  ): Json = {
    (v1.asObject, v2.asObject, v1.asArray, v2.asArray) match {
      // copied from circe's deepMerge method, however in order to handle concat + deduplication on arrays we need to do it manually here
      case (Some(lhs), Some(rhs), _, _) =>
        Json.fromJsonObject(
          lhs.toIterable.foldLeft(rhs) { case (acc, (key, value)) =>
            rhs(key).fold(acc.add(key, value)) { r =>
              acc.add(key, mergeJs(value, r))
            }
          }
        )
      case (_, _, Some(arr1), Some(arr2)) =>
        Json.arr((arr1 ++ arr2).distinct: _*)
      case _ => v1
    }
  }
}
