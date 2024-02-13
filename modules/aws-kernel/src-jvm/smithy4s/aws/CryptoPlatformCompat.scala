/*
 *  Copyright 2021-2024 Disney Streaming
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

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

trait CryptoPlatformCompat {
  type Binary = Array[Byte]

  def binaryFromString(str: String): Binary =
    str.getBytes("UTF-8")

  def toHexString(binary: Binary): String =
    binary.map("%02x".format(_)).mkString

  def sha256HexDigest(message: Array[Byte]): String = toHexString {
    MessageDigest
      .getInstance("SHA-256")
      .digest(message)
  }

  def sha256HexDigest(message: String): String =
    sha256HexDigest(message.getBytes("UTF-8"))

  def hmacSha256(data: String, key: Array[Byte]): Array[Byte] = {
    val algorithm = "HmacSHA256"
    val mac = Mac.getInstance(algorithm)
    mac.init(new SecretKeySpec(key, algorithm))
    mac.doFinal(data.getBytes("UTF-8"))
  }
}
