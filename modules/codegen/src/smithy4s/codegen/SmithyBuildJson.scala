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

import smithy4s.codegen.internals.SmithyBuild
import smithy4s.codegen.internals.SmithyBuildMaven

private[codegen] final case class SmithyBuildData(
    imports: Seq[String],
    deps: Seq[String],
    repos: Seq[String]
) {
  def addAll(
      imports: Seq[String],
      deps: Seq[String],
      repos: Seq[String]
  ): SmithyBuildData = {
    SmithyBuildData(
      this.imports ++ imports,
      this.deps ++ deps,
      this.repos ++ repos
    )
  }
}

object SmithyBuildJson {

  def toJson(
      imports: Seq[String],
      dependencies: Seq[String],
      repositories: Seq[String]
  ): String = {
    SmithyBuild.writeJson(
      SmithyBuild(
        version = "1.0",
        imports,
        SmithyBuildMaven(dependencies, repositories)
      )
    )
  }

  def merge(
      json1: String,
      json2: String
  ): String = {
    SmithyBuild.merge(json1, json2)
  }
}
