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

import scala.scalanative.unsafe._

@link("crypto")
@extern
object crypto {

  final val EVP_MAX_MD_SIZE = 64

  type EVP_MD
  type ENGINE

  /**
    * @return EVP_MD structures for SHA256 digest algorithm.
    */
  def EVP_sha256(): Ptr[EVP_MD] = extern

  def EVP_Digest(
      data: Ptr[Byte],
      count: CSize,
      md: Ptr[Byte],
      size: Ptr[CUnsignedInt],
      `type`: Ptr[EVP_MD],
      impl: Ptr[ENGINE]
  ): CInt = extern

  def HMAC(
      evp_md: Ptr[EVP_MD],
      key: Ptr[Byte],
      key_len: Int,
      d: Ptr[Byte],
      n: CSize,
      md: Ptr[Byte],
      md_len: Ptr[CUnsignedInt]
  ): Ptr[CUnsignedChar] = extern

  def ERR_get_error(): CUnsignedLong = extern
  def ERR_reason_error_string(e: CUnsignedLong): CString = extern

}
