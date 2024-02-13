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

import smithy4s.codegen.internals.SmithyBuild
import smithy4s.codegen.internals.SmithyBuildMaven
import smithy4s.codegen.internals.SmithyBuildMavenRepository
import upickle.default._

private[codegen] object SmithyBuildJson {

  def toJson(
      imports: Seq[String],
      dependencies: Seq[String],
      repositories: Seq[String]
  ): String = {
    SmithyBuild.writeJson(
      SmithyBuild(
        version = "1.0",
        imports,
        SmithyBuildMaven(
          dependencies,
          repositories.map(SmithyBuildMavenRepository.apply)
        )
      )
    )
  }

  def merge(
      json1: String,
      json2: String
  ): String = {
    val j1 = read[ujson.Value](json1)
    val j2 = read[ujson.Value](json2)
    val merged = mergeJs(j1, j2)
    val finalJs = removeArrayDuplicates(merged)
    finalJs.render(indent = 4)
  }

  private def mergeJs(
      v1: ujson.Value,
      v2: ujson.Value
  ): ujson.Value = {
    (v1, v2) match {
      case (ujson.Obj(obj1), ujson.Obj(obj2)) =>
        val result = obj2.foldLeft(obj1.toMap) {
          case (elements, (key, value2)) =>
            val value = elements.get(key) match {
              case None =>
                value2
              case Some(value1) =>
                mergeJs(value1, value2)
            }
            elements.updated(key, value)
        }
        ujson.Obj.from(result)
      case (arr1: ujson.Arr, arr2: ujson.Arr) =>
        ujson.Arr(arr1.arr ++ arr2.arr)
      case (_, _) => v1
    }
  }

  private def removeArrayDuplicates(js: ujson.Value): ujson.Value = {
    js match {
      case ujson.Obj(obj1) =>
        ujson.Obj.from(
          obj1.toList.map { case (key, value) =>
            key -> removeArrayDuplicates(value)
          }
        )
      case (arr1: ujson.Arr) => arr1.arr.distinct
      case x                 => x
    }
  }
}
