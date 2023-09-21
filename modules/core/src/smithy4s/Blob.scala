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
import scala.collection.immutable.Queue
import java.nio.charset.StandardCharsets
import java.io.OutputStream

// scalafmt: {maxColumn = 120}
sealed trait Blob {

  def apply(i: Int): Byte
  def size: Int

  def foreach(f: Byte => Unit) = {
    var i = 0
    while (i < size) { f(apply(i)); i += 1 }
  }

  def foreachWithIndex(f: (Byte, Int) => Unit) = {
    var i = 0
    while (i < size) { f(apply(i), i); i += 1 }
  }

  def toArray: Array[Byte] = {
    val result = Array.ofDim[Byte](size)
    foreachWithIndex((b, i) => result(i) = b)
    result
  }

  def toArrayUnsafe: Array[Byte] = toArray
  def isEmpty: Boolean

  def sameBytesAs(other: Blob): Boolean = {
    size == other.size && {
      var i = 0
      var result = true
      while (i < size && result) {
        result = this(i) == other(i)
        i += 1
      }
      result
    }
  }

  def asByteBuffer(offset: Int, size: Int): ByteBuffer = {
    val arr = new Array[Byte](size)
    copyToArray(arr, 0, offset, size)
    ByteBuffer.wrap(arr)
  }
  def asByteBuffer: ByteBuffer = asByteBuffer(0, size)
  def asByteBufferUnsafe(offset: Int, size: Int): ByteBuffer = asByteBuffer(offset, size)
  def asByteBufferUnsafe: ByteBuffer = asByteBuffer(0, size)

  def copyToArray(xs: Array[Byte], start: Int, offset: Int, size: Int): Unit = {
    var i = 0
    while (i < size) {
      xs(start + i) = apply(offset + i)
      i += 1
    }
  }

  def copyToBuffer(buffer: ByteBuffer, offset: Int, size: Int): Int = {
    var i = 0
    while (i < size && buffer.remaining > 0) {
      buffer.put(apply(offset + i))
      i += 1
    }
    i
  }

  def copyToStream(s: OutputStream, offset: Int, size: Int): Unit = {
    var i = 0
    while (i < size) {
      s.write(apply(offset + i).toInt)
      i += 1
    }
  }

  final def toBase64String: String = Base64.getEncoder().encodeToString(toArray)
  final def toUTF8String: String = new String(toArray, StandardCharsets.UTF_8)

  def concat(other: Blob) =
    if (this.isEmpty) other
    else
      this match {
        case qb: Blob.QueueBlob => new Blob.QueueBlob(qb.blobs :+ other, qb.size + other.size)
        case b                  => new Blob.QueueBlob(Queue(b, other), this.size + other.size)
      }

  final def ++(other: Blob) = concat(other)
}

object Blob {

  val empty: Blob = new Blob.ByteArrayBlob(Array.emptyByteArray)

  def apply(bytes: Array[Byte]): Blob = new ByteArrayBlob(bytes)
  def apply(buffer: ByteBuffer): Blob = new ByteBufferBlob(buffer)
  def apply(string: String): Blob = new ByteArrayBlob(
    string.getBytes(StandardCharsets.UTF_8)
  )

  final class ByteArrayBlob(val arr: Array[Byte]) extends Blob {

    def apply(i: Int) = arr(i.toInt)

    override def asByteBuffer(start: Int, size: Int): ByteBuffer =
      asByteBufferUnsafe(start, size).asReadOnlyBuffer()

    override def asByteBufferUnsafe(start: Int, size: Int): ByteBuffer = {
      val b = ByteBuffer.wrap(arr, start.toInt, size)
      if (start == 0 && size == arr.length) b
      else b.slice()
    }

    override def copyToArray(xs: Array[Byte], start: Int, offset: Int, size: Int): Unit =
      System.arraycopy(arr, offset.toInt, xs, start, size)

    override def copyToStream(s: OutputStream, offset: Int, size: Int): Unit =
      s.write(arr, offset.toInt, size.toInt)

    override def copyToBuffer(buffer: ByteBuffer, offset: Int, size: Int): Int = {
      val toCopy = buffer.remaining.min(size)
      buffer.put(arr, offset.toInt, toCopy)
      toCopy
    }

    override def toArray: Array[Byte] = {
      val newArray = Array.ofDim[Byte](arr.length)
      arr.copyToArray(newArray, 0, arr.length)
      newArray
    }
    override def toArrayUnsafe: Array[Byte] = arr

    override def isEmpty: Boolean = arr.isEmpty
    override def size: Int = arr.length

    override def toString = {
      s"ByteArrayBlob[${Base64.getEncoder().encodeToString(arr)}]"
    }

    override def equals(other: Any): Boolean = {
      other.isInstanceOf[ByteArrayBlob] &&
      java.util.Arrays.equals(arr, other.asInstanceOf[ByteArrayBlob].arr)
    }

    override def hashCode(): Int = {
      var hashCode = 0
      var i = 0
      while (i < arr.length) {
        hashCode += arr(i).hashCode()
        i += 1
      }
      hashCode
    }
  }
  final class ByteBufferBlob(val buf: ByteBuffer) extends Blob {
    def apply(i: Int) = buf.get(i.toInt)

    override def copyToArray(xs: Array[Byte], start: Int, offset: Int, size: Int): Unit = {
      val n = buf.duplicate()
      n.position(offset.toInt)
      n.get(xs, start, size)
      ()
    }

    override def toArray: Array[Byte] = {
      val arr = Array.ofDim[Byte](buf.remaining())
      copyToArray(arr, 0, 0, size)
      arr
    }

    override def asByteBuffer(offset: Int, size: Int): ByteBuffer =
      asByteBufferUnsafe(offset, size).asReadOnlyBuffer()

    override def asByteBufferUnsafe(offset: Int, size: Int): ByteBuffer = {
      val b = buf
      if (offset == 0 && b.position() == 0 && size == b.remaining()) b
      else {
        b.position(offset.toInt)
        b.limit(offset.toInt + size)
        b.slice()
      }
    }

    override def asByteBufferUnsafe: ByteBuffer = buf

    override def copyToBuffer(buffer: ByteBuffer, offset: Int, size: Int): Int = {
      val toCopy = buffer.remaining.min(size)
      buffer.put(asByteBuffer(offset, toCopy))
      toCopy
    }

    override def toString = s"ByteBufferBlob[${buf.toString()}]"
    override def isEmpty: Boolean = !buf.hasRemaining()
    override def size: Int = buf.remaining()
    override def hashCode = buf.hashCode()

    override def equals(other: Any): Boolean = {
      other.isInstanceOf[ByteBufferBlob] &&
      buf.compareTo(other.asInstanceOf[ByteBufferBlob].buf) == 0
    }
  }

  final class ArraySliceBlob(val arr: Array[Byte], val offset: Int, val length: Int) extends Blob {

    require(
      offset >= 0 && offset <= arr.size && length >= 0 && length <= arr.size && offset + length <= arr.size
    )
    def apply(i: Int): Byte =
      if (i >= length) throw new IndexOutOfBoundsException()
      else arr((offset + i).toInt)

    def size: Int = length
    def isEmpty: Boolean = (length == 0)

    override def toArray: Array[Byte] = {
      val result = Array.ofDim[Byte](length)
      arr.copyToArray(arr, offset, length)
      result
    }

    override def hashCode(): Int = {
      import util.hashing.MurmurHash3
      var h = MurmurHash3.stringHash("ArraySliceBlob")
      h = MurmurHash3.mix(h, MurmurHash3.arrayHash(arr))
      h = MurmurHash3.mix(h, offset)
      MurmurHash3.mixLast(h, length)
    }

    override def equals(other: Any): Boolean = {
      other.isInstanceOf[ArraySliceBlob] && {
        val o = other.asInstanceOf[ArraySliceBlob]
        offset == o.offset &&
        length == o.length &&
        java.util.Arrays.equals(arr, o.arr)
      }
    }

  }

  final class QueueBlob private[smithy4s] (val blobs: Queue[Blob], val size: Int) extends Blob {
    def apply(i: Int): Byte = {
      if (i > size) throw new IndexOutOfBoundsException()
      else {
        var localIndex = i
        var (currentHead, currentTail) = blobs.dequeue
        while (localIndex > currentHead.size) {
          localIndex = localIndex - currentHead.size
          val dq = currentTail.dequeue
          currentHead = dq._1
          currentTail = dq._2
        }
        currentHead(localIndex)
      }
    }
    override def foreach(f: Byte => Unit): Unit =
      blobs.foreach(
        _.foreach(f)
      )
    override def foreachWithIndex(f: (Byte, Int) => Unit): Unit = {
      var i = 0
      blobs.foreach { blob =>
        blob.foreach { byte => f(byte, i); i = i + 1 }
      }
    }
    def isEmpty: Boolean = size == 0
  }

}
