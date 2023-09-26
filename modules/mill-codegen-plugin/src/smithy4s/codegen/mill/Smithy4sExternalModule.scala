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

package smithy4s.codegen.mill

import mill.eval.Evaluator
import mill.define.ExternalModule
import mill.define.Target
import coursier.maven.MavenRepository
import smithy4s.codegen.SmithyBuildJson
import mill.define.Command
import mill.api.PathRef

object Smithy4sExternalModule extends ExternalModule {
  lazy val millDiscover = mill.define.Discover[this.type]

  def generateSmithyBuildJson(ev: Evaluator): Command[PathRef] =
    Target.command {
      val rootPath = ev.rootModule.millModuleBasePath.value
      val s4sModules = ev.rootModule.millInternal.modules
        .collect { case s: Smithy4sModule => s }

      val deps = ev
        .evalOrThrow()(s4sModules.map(_.smithy4sAllDeps))
        .flatMap(_.flatMap(Smithy4sModule.depIdEncode))
        .distinct
      val repos = ev
        .evalOrThrow()(s4sModules.map(_.repositoriesTask))
        .flatMap(_.collect {
          case r: MavenRepository if !r.root.contains("repo1.maven.org") =>
            r.root
        })
        .distinct
      val imports = ev
        .evalOrThrow()(s4sModules.map(_.smithy4sInputDirs))
        .flatMap(
          _.map(p => p.path.relativeTo(rootPath))
            .map(_.toString)
        )
        .distinct

      val json = SmithyBuildJson.toJson(imports, deps, repos)
      val target = rootPath / "smithy-build.json"
      os.write.over(target, json)
      PathRef(target)
    }
}
