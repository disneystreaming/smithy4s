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

import _root_.{mill => mmill}
import coursier.maven.MavenRepository
import mmill.api.PathRef
import mmill.define.Command
import mmill.define.ExternalModule
import mmill.define.Target
import mmill.eval.Evaluator
import smithy4s.codegen.SmithyBuildJson
import smithy4s.codegen.mill.Smithy4sModule

object LSP extends ExternalModule {
  lazy val millDiscover = mmill.define.Discover[this.type]

  def updateConfig(ev: Evaluator): Command[PathRef] = {
    val rootPath = ev.rootModule.millModuleBasePath.value
    val s4sModules = ev.rootModule.millInternal.modules
      .collect { case s: Smithy4sModule => s }

    val depsTask = Target
      .traverse(s4sModules)(_.smithy4sAllDeps)
      .map(_.flatten.flatMap(Smithy4sModule.depIdEncode(_)).distinct)

    val reposTask = Target
      .traverse(s4sModules)(_.repositoriesTask)
      .map {
        _.flatten.collect {
          case r: MavenRepository if !r.root.contains("repo1.maven.org") =>
            r.root
        }.distinct
      }

    val importsTask = Target
      .traverse(s4sModules)(_.smithy4sInputDirs)
      .map(
        _.flatten
          .map(p => p.path.relativeTo(rootPath))
          .map(rp => "./" + rp.toString)
          .distinct
      )

    Target.command {
      val json = SmithyBuildJson.toJson(importsTask(), depsTask(), reposTask())
      val target = rootPath / "smithy-build.json"
      val content = if (os.exists(target)) {
        val content = os.read(target)
        SmithyBuildJson.merge(content, json)
      } else json
      os.write.over(target, content, createFolders = true)
      PathRef(target)
    }
  }
}
