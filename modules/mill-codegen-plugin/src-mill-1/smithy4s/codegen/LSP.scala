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

import _root_.{mill => mmill}
import coursier.maven.MavenRepository
import mmill.api.BuildCtx
import mmill.api.Discover
import mmill.api.Evaluator
import mmill.api.ExternalModule
import mmill.api.PathRef
import mmill.api.SelectMode
import mmill.api.Task
import mmill.util.TokenReaders.given
import smithy4s.codegen.SmithyBuildJson
import smithy4s.codegen.mill.LSPCompat
import smithy4s.codegen.mill.Smithy4sModule

import scala.collection.immutable.ListSet

object LSP extends ExternalModule with LSPCompat {
  lazy val millDiscover: Discover = Discover[this.type]

  def updateConfig(ev: Evaluator): Task.Command[PathRef] = {
    val rootPath = BuildCtx.workspaceRoot

    val effectiveModules: Seq[Smithy4sModule] = {
      ev.resolveModulesOrTasks(
        Seq("__.smithy4sAllDeps"),
        SelectMode.Multi
      ) match {
        case mmill.api.Result.Success(items) =>
          val modules = items.collect { case Left(m: Smithy4sModule) => m }
          if (modules.nonEmpty) modules
          else
            ev.resolveModulesOrTasks(Seq("__"), SelectMode.Multi) match {
              case mmill.api.Result.Success(all) =>
                all.collect { case Left(m: Smithy4sModule) => m }
              case _ => Seq.empty
            }
        case _ => Seq.empty
      }
    }

    val depsTask = Task
      .traverse(effectiveModules)(_.smithy4sAllDeps)
      .map(_.flatten.flatMap(Smithy4sModule.depIdEncode(_)))
      .map(s => ListSet(s*))

    val reposTask = Task
      .traverse(effectiveModules)(_.repositoriesTask)
      .map {
        _.flatten.collect {
          case r: MavenRepository if !r.root.contains("repo1.maven.org") =>
            r.root
        }
      }
      .map(s => ListSet(s*))

    val importsTask = Task
      .traverse(effectiveModules)(_.smithy4sInputDirs)
      .map(
        _.flatten
          .map(p => p.path.relativeTo(rootPath))
          .map(rp => "./" + rp.toString)
      )
      .map(s => ListSet(s*))

    Task.Command {
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
