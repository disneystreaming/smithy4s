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

import java.nio.ByteBuffer
import java.util.Base64
import java.nio.charset.StandardCharsets

sealed trait Blob {
  def toArray: Array[Byte]
  def buffer: ByteBuffer
  def isEmpty: Boolean
  def size: Int
  def sameBytesAs(other: Blob): Boolean
  final def toArrayBlob: Blob = Blob(toArray)
  final def toBase64String: String = Base64.getEncoder().encodeToString(toArray)
  final def toUTF8String: String = new String(toArray, StandardCharsets.UTF_8)
}

object Blob {

  val empty: Blob = new Blob.ByteArrayBlob(Array.emptyByteArray)

  def apply(bytes: Array[Byte]): Blob = new ByteArrayBlob(bytes)
  def apply(buffer: ByteBuffer): Blob = new ByteBufferBlob(buffer)
  def apply(string: String): Blob = new ByteArrayBlob(
    string.getBytes(StandardCharsets.UTF_8)
  )

  private final class ByteArrayBlob(private val bytes: Array[Byte])
      extends Blob {
    override def toArray: Array[Byte] = bytes
    override def isEmpty: Boolean = bytes.isEmpty
    override def size: Int = bytes.length
    override def buffer: ByteBuffer =
      ByteBuffer.wrap(java.util.Arrays.copyOf(bytes, bytes.length))

    override def toString = {
      s"ByteArrayBlob[${Base64.getEncoder().encodeToString(bytes)}]"
    }

    override def sameBytesAs(other: Blob): Boolean = other match {
      case otherBlob: ByteArrayBlob =>
        java.util.Arrays.equals(bytes, otherBlob.bytes)
      case otherBlob: ByteBufferBlob =>
        ByteBuffer.wrap(bytes).compareTo(otherBlob.buffer) == 0
    }

    override def equals(other: Any): Boolean = {
      other.isInstanceOf[ByteArrayBlob] &&
      java.util.Arrays.equals(bytes, other.asInstanceOf[ByteArrayBlob].bytes)
    }

    override def hashCode(): Int = {
      var hashCode = 0
      var i = 0
      while (i < bytes.length) {
        hashCode += bytes(i).hashCode()
        i += 1
      }
      hashCode
    }
  }
  private final class ByteBufferBlob(val buffer: ByteBuffer) extends Blob {
    override def toString = s"ByteBufferBlob[${buffer.toString()}]"
    override def isEmpty: Boolean = !buffer.hasRemaining()
    override def size: Int = buffer.remaining()
    override def hashCode = buffer.hashCode()
    override def sameBytesAs(other: Blob): Boolean = other match {
      case otherBlob: ByteBufferBlob =>
        buffer.compareTo(otherBlob.buffer) == 0
      case otherBlob: ByteArrayBlob =>
        buffer.compareTo(ByteBuffer.wrap(otherBlob.toArray)) == 0
    }

    override def equals(other: Any): Boolean = {
      other.isInstanceOf[ByteArrayBlob] &&
      buffer.compareTo(other.asInstanceOf[ByteBufferBlob].buffer) == 0
    }

    private var arr: Array[Byte] = null

    def toArray: Array[Byte] = {
      if (arr == null) {
        this.synchronized {
          if (arr == null) {
            arr = Array.ofDim[Byte](buffer.remaining())
            val _ = buffer.get(arr)
          }
        }
      }
      arr
    }
  }

}
