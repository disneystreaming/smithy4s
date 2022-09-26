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

package smithy4s.aws.kernel

import scala.scalanative.unsafe
import scala.scalanative.unsafe._
import scala.scalanative.unsigned._

trait CryptoPlatformCompat {
  type Binary = Array[Byte]

  def binaryFromString(str: String): Binary =
    str.getBytes("UTF-8")

  def toHexString(binary: Binary): String =
    binary.map("%02x".format(_)).mkString

  def sha256HexDigest(message: Array[Byte]): String =
    unsafe.Zone { implicit z =>
      val outLength: Ptr[CUnsignedInt] = unsafe.alloc[CUnsignedInt]()
      val digest: Ptr[CUnsignedChar] =
        unsafe.alloc[CUnsignedChar](CryptoPlatformCompat.EVP_MAX_MD_SIZE)
      val dataLen = message.length.toLong
      val cInput: CString = arrayToCString(message)

      val ctx = crypto.EVP_MD_CTX_new()
      crypto.EVP_DigestInit(ctx, crypto.EVP_sha256())
      crypto.EVP_DigestUpdate(ctx, cInput, dataLen.toUInt)
      crypto.EVP_DigestFinal(ctx, digest, outLength)

      val out = Array.fill[Byte]((!outLength).toInt)(0)
      var i = 0
      while (i < (!outLength).toInt) {
        out(i) = digest(i.toLong).toByte
        i += 1
      }

      out.map("%02x".format(_)).mkString
    }

  def sha256HexDigest(message: String): String = sha256HexDigest(
    message.getBytes("UTF-8")
  )

  def hmacSha256(data: String, key: Binary): Binary =
    unsafe.Zone { implicit z =>
      val outLength: Ptr[CUnsignedInt] = unsafe.alloc[CUnsignedInt]()
      val digest: Ptr[CUnsignedChar] =
        unsafe.alloc[CUnsignedChar](CryptoPlatformCompat.EVP_MAX_MD_SIZE)

      val cKey = arrayToCString(key)
      val keylen = key.length.toLong

      val cData: CString = toCString(data)
      val datalen = data.length.toUInt

      crypto.HMAC(
        crypto.EVP_sha256(),
        cKey,
        keylen,
        cData,
        datalen,
        digest,
        outLength
      )

      val out = Array.fill[Byte]((!outLength).toInt)(0)
      var i = 0

      while (i < (!outLength).toInt) {
        out(i) = digest(i.toLong).toByte
        i += 1
      }
      out
    }

  private def arrayToCString(bytes: Array[Byte])(implicit z: Zone): CString = {
    val cstr = z.alloc((bytes.length.toLong + 1).toUInt)
    var c = 0
    while (c < bytes.length) {
      !(cstr + c.toLong) = bytes(c)
      c += 1
    }
    !(cstr + c.toLong) = 0.toByte
    cstr
  }
}

object CryptoPlatformCompat {
  val EVP_MAX_MD_SIZE = 64L
}
