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

package smithy4s.codegen.internals

private[codegen] object EnumUtil {
  def enumValueClassName(
      name: Option[String],
      value: String,
      intValue: Int
  ) = {
    name.getOrElse {
      val camel = toCamelCase(value).capitalize
      if (camel.nonEmpty) camel else "Value" + intValue
    }
  }

  private def toCamelCase(value: String): String = {
    val (_, output) = value.foldLeft((false, "")) {
      case ((wasLastSkipped, str), c) =>
        if (c.isLetterOrDigit) {
          val newC =
            if (wasLastSkipped) c.toString.capitalize else c.toString
          (false, str + newC)
        } else {
          (true, str)
        }
    }
    output
  }

}
