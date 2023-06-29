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

import smithy4s.http.internals.vectorOps

package object uri {
  private def extractHostLabels(str: String): Option[HostPrefixSegment] = {
    if (str == null || str.isEmpty) None
    else if (str.startsWith("{") && str.endsWith("}"))
      Some(HostPrefixSegment.label(str.substring(1, str.length() - 1)))
    else Some(HostPrefixSegment.static(str))
  }

  private[smithy4s] def hostPrefixSegments(
      str: String
  ): Vector[HostPrefixSegment] = {
    str
      .split('.')
      .toVector
      .filterNot(_.isEmpty())
      .traverse(extractHostLabels(_))
      .getOrElse(Vector.empty)
  }
}
