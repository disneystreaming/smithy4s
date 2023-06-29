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

package smithy4s.http

package object uri {
  /*  private def extractHostLabels(str: String): Option[HostPrefixSegment] = {
    if (str == null || str.isEmpty) None
    else if (str.startsWith("{") && str.endsWith("}"))
      Some(HostPrefixSegment.label(str.substring(1, str.length() - 1)))
    else Some(HostPrefixSegment.static(str))
  }*/

  private[smithy4s] def hostPrefixSegments(
      str: String
  ): Vector[HostPrefixSegment] = {
    // example input: "foo.{bar}--{baz}abcd{test}.com" produces the following
    // output: Vector(static(foo.), label(bar), static(--), label(baz), static(abcd), label(test), static(.com))
    str
      .split('{')
      .toList
      .flatMap(_.split("}", 2).toList match {
        case static :: Nil => HostPrefixSegment.static(static) :: Nil
        case label :: static :: Nil =>
          HostPrefixSegment
            .label(label) :: HostPrefixSegment.static(static) :: Nil
        case _ => Nil
      })
      .toVector

  }
}
