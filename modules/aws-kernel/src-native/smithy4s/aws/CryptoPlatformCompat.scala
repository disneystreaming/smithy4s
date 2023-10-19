/*
 *  Copyright 2021-2023 Disney Streaming
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

import scala.scalanative.unsafe._
import scala.scalanative.unsigned._

import crypto._

trait CryptoPlatformCompat {
  type Binary = Array[Byte]

  private[this] val EvpMdSha256 = EVP_sha256()

  def binaryFromString(str: String): Binary =
    str.getBytes("UTF-8")

  def toHexString(binary: Binary): String =
    binary.map("%02x".format(_)).mkString

  def sha256HexDigest(message: Array[Byte]): String = {
    val md = new Array[Byte](EVP_MAX_MD_SIZE)
    val size = stackalloc[CUnsignedInt]()
    if (
      EVP_Digest(
        message.atUnsafe(0),
        message.length.toULong,
        md.atUnsafe(0),
        size,
        EvpMdSha256,
        null
      ) != 1
    )
      throw new RuntimeException(s"EVP_Digest: ${getError()}")

    toHexString(md.take((!size).toInt))
  }

  def sha256HexDigest(message: String): String = sha256HexDigest(
    message.getBytes("UTF-8")
  )

  def hmacSha256(data: String, key: Binary): Binary = {
    val md = new Array[Byte](EVP_MAX_MD_SIZE)
    val mdLen = stackalloc[CUnsignedInt]()
    val d = data.getBytes("UTF-8")
    if (
      HMAC(
        EvpMdSha256,
        key.atUnsafe(0),
        key.size.toInt,
        d.atUnsafe(0),
        d.length.toULong,
        md.atUnsafe(0),
        mdLen
      ) == null
    )
      throw new RuntimeException(s"HMAC: ${getError()}")
    md.take((!mdLen).toInt)
  }

  private[this] def getError(): String =
    fromCString(ERR_reason_error_string(ERR_get_error()))

}
