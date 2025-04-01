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

import munit.diff.Diff
import sbt._
import sbt.util.CacheImplicits._
import sbt.util.CacheStore
import sbt.util.Logger
import sjsonnew._
import sjsonnew.support.murmurhash.Hasher
import sjsonnew.support.scalajson.unsafe.Converter
import sjsonnew.support.scalajson.unsafe.{PrettyPrinter => prettify}

import scala.util.Try

private[codegen] object CachedTask {

  // This implementation is inspired by sbt.util.Tracked.inputChanged
  // The main difference is that when the values don't match, the difference is calculated
  // using munit-diff and recorded to debug log
  def inputChanged[I: JsonFormat, O](store: CacheStore, logger: Logger)(
      f: (Boolean, I) => O
  ): I => O = { in =>
    def debug(str: String): Unit = logger.debug(s"[smithy4s] $str")

    val previousValue = Try(store.read[ValueAndHash[I]]()).toOption
    val newValueHash = hash(in)
    store.write[ValueAndHash[I]]((in, newValueHash))

    previousValue match {
      case None =>
        debug("Could not read previous inputs value from cache.")
        f(true, in)

      case Some((oldValue, previousHash)) =>
        (toJson(oldValue), toJson(in)) match {
          case (Some(oldArgs), Some(newArgs)) if !oldArgs.equals(newArgs) =>
            val diff = new Diff(prettify(oldArgs), prettify(newArgs))
            val report = diff.createReport(
              "Arguments changed between smithy4s codegen invocations, diff:",
              printObtainedAsStripMargin = false
            )
            debug(report)
            f(true, in)

          case (_, _) if (previousHash != newValueHash) =>
            debug(
              "Codegen arguments didn't change, but their hashes didn't match. " +
                "This means file change on paths provided as codegen arguments."
            )
            f(true, in)

          case _ =>
            debug("Input didn't change between codegen invocations")
            f(false, in)
        }

    }
  }

  private type ValueAndHash[I] = (I, Int)

  private def toJson[I: JsonFormat](args: I) =
    Converter.toJson(args).toOption

  private def hash[I: JsonFormat](in: I) =
    Hasher.hash(in).toOption.getOrElse(-1)
}
