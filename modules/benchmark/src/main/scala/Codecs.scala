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

package smithy4s.benchmark

import io.circe._
import smithy4s._
import smithy4s.Timestamp

import java.util.Base64

object Circe {
  implicit val blobDecoder: Decoder[Blob] =
    Decoder.decodeString.map(s => Blob(Base64.getDecoder().decode(s)))

  implicit val blobEncoder: Encoder[Blob] =
    Encoder.encodeString.contramap[Blob](_.toBase64String)

  implicit val timestampDecoder: Decoder[Timestamp] =
    Decoder.decodeLong.map(Timestamp(_, 0))

  implicit val timestampEncoder: Encoder[Timestamp] =
    Encoder.encodeLong.contramap(_.epochSecond)

  implicit val permissionCodec: Codec[Permission] =
    io.circe.generic.semiauto.deriveCodec[Permission]

  implicit val metadataCodec: Codec[Metadata] =
    io.circe.generic.semiauto.deriveCodec[Metadata]

  implicit val credsCodec: Codec[Creds] =
    io.circe.generic.semiauto.deriveCodec[Creds]

  implicit val encryptionMetadataCodec: Codec[EncryptionMetadata] =
    io.circe.generic.semiauto.deriveCodec[EncryptionMetadata]

  implicit val encryptionCodec: Codec[Encryption] =
    io.circe.generic.semiauto.deriveCodec[Encryption]

  implicit val attributesCodec: Codec[Attributes] =
    io.circe.generic.semiauto.deriveCodec[Attributes]

  implicit val s3ObjectCodec: Codec[S3Object] =
    io.circe.generic.semiauto.deriveCodec[S3Object]
}
