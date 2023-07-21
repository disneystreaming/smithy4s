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

import java.util.Base64

@deprecated("Use smithy4s.Blob instead", since = "0.18.0")
@annotation.nowarn("msg=Use smithy4s.Blob instead")
class ByteArray(val array: Array[Byte]) {
  override def equals(other: Any) = other match {
    case bytes: ByteArray => java.util.Arrays.equals(array, bytes.array)
    case _                => false
  }

  override def hashCode(): Int = {
    var hashCode = 0
    var i = 0
    while (i < array.length) {
      hashCode += array(i).hashCode()
      i += 1
    }
    hashCode
  }

  override def toString = Base64.getEncoder().encodeToString(array)
}

@annotation.nowarn("msg=Use smithy4s.Blob instead")
object ByteArray {
  val empty = new ByteArray(Array.emptyByteArray)

  def apply(array: Array[Byte]) = new ByteArray(array)
}
