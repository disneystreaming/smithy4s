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

package smithy4s

import cats.MonadThrow

package object aws {

  type Timestamp = smithy4s.Timestamp
  val Timestamp = smithy4s.Timestamp
  type AwsRegion = aws.kernel.AwsRegion
  val AwsRegion = aws.kernel.AwsRegion
  type AwsCredentials = aws.kernel.AwsCredentials
  val AwsCredentials = aws.kernel.AwsCredentials

  type AwsClient[Alg[_[_, _, _, _, _]], F[_]] = Alg[AwsCall[F, *, *, *, *, *]]

  private[aws] def utf8String[F[_]: MonadThrow](bytes: Array[Byte]): F[String] =
    MonadThrow[F].catchNonFatal(new String(bytes, "UTF-8"))

}
