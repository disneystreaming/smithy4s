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

package smithy4s.codegen.mill

import mill._
import mill.api.PathRef
import mill.define.Target
import mill.scalalib.CrossVersion.Binary
import mill.scalalib.CrossVersion.Constant
import mill.scalalib.CrossVersion.Full
import mill.scalalib._
import smithy4s.codegen.JarUtils

trait Smithy4sModule extends Smithy4sModuleCommon {

  /** Input directory for .smithy files */
  def smithy4sInputDirs: Target[Seq[PathRef]] = T.sources {
    Seq(PathRef(moduleDir / "smithy"))
  }

  def smithy4sExternallyTrackedIvyDeps: T[Agg[Dep]] = T {
    resolveDeps(T { allIvyDeps().map(bindDependency()) })()
      .flatMap { pathRef =>
        val deps = JarUtils
          .extractSmithy4sDependencies(pathRef.path.toIO)
          .map(dep => ivy"$dep")
        Agg.from(deps)
      }
  }
}

object Smithy4sModule {
  def depIdEncode(dep: Dep): Option[String] = {
    val mod = dep.dep.module
    val org = mod.organization.value
    val name = mod.name.value
    val version = dep.dep.versionConstraint.asString
    dep.cross match {
      case Binary(_)      => Some(s"$org::$name:$version")
      case Constant(_, _) => Some(s"$org:$name:$version")
      case Full(_)        => None
    }
  }
}
