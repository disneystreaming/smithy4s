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

@link("crypto")
@unsafe.extern
object crypto {

  /**
    * Allocates, initializes and returns a digest context.
    */
  def EVP_MD_CTX_new(): Ptr[EVP_MD_CTX] = extern

  /**
    * @return EVP_MD structures for SHA256 digest algorithm.
    */
  def EVP_sha256(): Ptr[EVP_MD] = extern

  def EVP_DigestInit(ctx: Ptr[EVP_MD_CTX], md: Ptr[EVP_MD]): Unit = extern

  def EVP_DigestUpdate(
      ctx: Ptr[EVP_MD_CTX],
      data: Ptr[CSignedChar],
      datalen: unsafe.CSize
  ): CInt = extern

  def EVP_DigestFinal(
      ctx: Ptr[EVP_MD_CTX],
      res: Ptr[CUnsignedChar],
      reslen: Ptr[CUnsignedInt]
  ): Unit = extern

  def HMAC(
      md: Ptr[EVP_MD],
      key: CString,
      keylen: CSSize,
      data: Ptr[CSignedChar],
      datalen: CSize,
      res: Ptr[CUnsignedChar],
      reslen: Ptr[CUnsignedInt]
  ): Unit = extern

}

/**
  * The EVP_MD type is a structure for digest method implementation
  */
trait EVP_MD {}

trait EVP_MD_CTX {}
