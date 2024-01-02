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
package internals

import upickle.default._

private[internals] final case class SmithyBuild(
    version: String,
    imports: Seq[String],
    maven: SmithyBuildMaven
)
private[codegen] object SmithyBuild {
  implicit val codecs: ReadWriter[SmithyBuild] = macroRW
  def writeJson(sb: SmithyBuild): String = write(sb, indent = 4)
}

private[internals] final case class SmithyBuildMaven(
    dependencies: Seq[String],
    repositories: Seq[SmithyBuildMavenRepository]
)
private[codegen] object SmithyBuildMaven {
  implicit val codecs: ReadWriter[SmithyBuildMaven] = macroRW
}

private[internals] final case class SmithyBuildMavenRepository(
    url: String
)
private[codegen] object SmithyBuildMavenRepository {
  implicit val codecs: ReadWriter[SmithyBuildMavenRepository] = macroRW
}
