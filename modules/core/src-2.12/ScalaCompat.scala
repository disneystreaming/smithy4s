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

package smithy4s

import scala.util.Try

private[smithy4s] trait ScalaCompat {
  implicit final class SmithyStringOps(val s: String) {
    def toIntOption: Option[Int] = Try(s.toInt).toOption
    def toDoubleOption: Option[Double] = Try(s.toDouble).toOption
    def toLongOption: Option[Long] = Try(s.toLong).toOption
    def toFloatOption: Option[Float] = Try(s.toFloat).toOption
    def toShortOption: Option[Short] = Try(s.toShort).toOption
    def toBooleanOption: Option[Boolean] = Try(s.toBoolean).toOption
  }

  implicit final class MapOps[K, V](val map: Map[K, V]) {
    def mapToValues[W](f: V => W): Map[K, W] = map.mapValues(f)
  }

}
