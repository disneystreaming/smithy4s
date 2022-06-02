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

import CryptoPlatformCompat._

trait CryptoPlatformCompat {
  type Binary = Buffer

  def binaryFromString(str: String): Binary = Buffer.from(str, utf8)

  def toHexString(binary: Binary): String =
    binary
      .entries()
      .toIterator
      .flatMap(_.lastOption)
      .map(n => f"$n%02x")
      .mkString

  def sha256HexDigest(message: Array[Byte]): String = {
    this.sha256HexDigest(new String(message))
  }

  def sha256HexDigest(message: String): String = {
    val hash = Crypto.createHash(sha256)
    hash.update(message, utf8)
    hash.digest(hex)
  }

  def hmacSha256(data: String, key: Binary): Binary = {
    val hmac = Crypto.createHmac(sha256, key)
    hmac.update(data)
    hmac.digest()
  }

}

object CryptoPlatformCompat {
  val sha256 = "sha256"
  val utf8 = "UTF-8"
  val hex = "hex"
  val binary = "binary"
  val ascii = "ascii"
}
