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

private[codegen] object Compat {
  type SettingsMap = sbt.internal.util.Settings[sbt.Scope]

  // sbt 1 has no task caching, so Def.uncached is a no-op.
  // This implicit class adds the method to match sbt 2's native API.
  implicit class DefOps(private val singleton: sbt.Def.type) extends AnyVal {
    def uncached[A](a: A): A = a
  }
}
