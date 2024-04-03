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

package smithy4s.aws

import scala.annotation.implicitNotFound
import smithy4s.Bijection

object AwsOperationKind {

  @implicitNotFound(
    "Cannot prove that the operation has simple request/response semantics: it has (a) streamed component(s)."
  )
  sealed trait Unary[StreamedInput, StreamedOutput]
  object Unary {
    implicit val unary: Unary[Nothing, Nothing] = new Unary[Nothing, Nothing] {}
  }

  @implicitNotFound(
    "Cannot prove that the operation is a blob upload. No instance of ByteUpload[${StreamedInput}, ${StreamedOutput}]"
  )
  trait ByteUpload[StreamedInput, StreamedOutput]
  object ByteUpload {
    implicit val ByteUpload: ByteUpload[Byte, Nothing] =
      new ByteUpload[Byte, Nothing] {}
  }

  /**
   * Removed the sealed because I'm unable to define an instance for StreamedBlob that's
   * found by implicit search
   */
  @implicitNotFound(
    "Cannot prove that the operation is a blob download. No instance of ByteDownload[${StreamedInput}, ${StreamedOutput}"
  )
  sealed trait ByteDownload[StreamedInput, StreamedOutput] {
    def apply(value: Byte): StreamedOutput = value.asInstanceOf[StreamedOutput]
  }
  object ByteDownload {
    implicit val ByteDownload: ByteDownload[Nothing, Byte] =
      new ByteDownload[Nothing, Byte] {}
    implicit def fromBijection[T](implicit
        ev: Bijection[Byte, T]
    ): ByteDownload[Nothing, T] =
      new ByteDownload[Nothing, T]() { locally(ev) }
  }
}
