package smithy4s

import java.nio.ByteBuffer
import java.util.Base64
import java.nio.charset.StandardCharsets

sealed trait Blob {
  def toArray: Array[Byte]
  def isEmpty: Boolean
  final def toArrayBlob: Blob = Blob(toArray)
  final def toBase64String: String = Base64.getEncoder().encodeToString(toArray)
  final def toUTF8String: String = new String(toArray, StandardCharsets.UTF_8)
}

object Blob {

  val empty: Blob = new Blob.ByteArrayBlob(Array.emptyByteArray)

  def apply(bytes: Array[Byte]): Blob = new ByteArrayBlob(bytes)
  def apply(buffer: ByteBuffer): Blob = new ByteBufferBlob(buffer)
  def apply(string: String): Blob = new ByteArrayBlob(string.getBytes())

  private final class ByteArrayBlob(private val bytes: Array[Byte])
      extends Blob {
    def toArray: Array[Byte] = bytes
    def isEmpty: Boolean = bytes.isEmpty

    override def toString = {
      s"ByteArrayBlob[${Base64.getEncoder().encodeToString(bytes)}]"
    }

    override def equals(other: Any) = other match {
      case otherBlob: ByteArrayBlob =>
        java.util.Arrays.equals(bytes, otherBlob.bytes)
      case _ => false
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
  private final class ByteBufferBlob(private val buffer: ByteBuffer)
      extends Blob {
    override def toString = s"ByteBufferBlob[${buffer.toString()}]"
    override def isEmpty: Boolean = !buffer.hasRemaining()
    override def hashCode = buffer.hashCode()
    override def equals(other: Any) = other match {
      case otherBlob: ByteBufferBlob => buffer == otherBlob.buffer
      case _                         => false
    }

    private val arr: Array[Byte] = null
    def toArray: Array[Byte] = {
      if (arr == null) {
        this.synchronized {
          if (arr == null) {
            val arr: Array[Byte] =
              Array.ofDim[Byte](buffer.remaining())
            val _ = buffer.get(arr)
          }
        }
      }
      arr
    }
  }

}
