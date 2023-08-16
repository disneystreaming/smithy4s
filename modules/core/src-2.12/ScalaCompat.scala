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

private[smithy4s] trait ScalaCompat {
  private[smithy4s] implicit final class SmithyStringOps(val s: String) {
    def toIntOption: Option[Int] = opt(s.toInt)
    def toDoubleOption: Option[Double] = opt(s.toDouble)
    def toLongOption: Option[Long] = opt(s.toLong)
    def toFloatOption: Option[Float] = opt(s.toFloat)
    def toShortOption: Option[Short] = opt(s.toShort)
    def toBooleanOption: Option[Boolean] = opt(s.toBoolean)
    def toByteOption: Option[Byte] = opt(s.toByte)
    private def opt[A](a: => A): Option[A] = try { Some(a) }
    catch { case scala.util.control.NonFatal(_) => None }
  }
}
