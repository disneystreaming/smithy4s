/*
 *  Copyright 2021 Disney Streaming
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
import schematic._
import smithy4s.Timestamp

import java.util.Base64

object Circe {
  implicit val byteArrayDecoder: Decoder[ByteArray] =
    Decoder.decodeString.map(s => ByteArray(Base64.getDecoder().decode(s)))

  implicit val byteArrayEncoder: Encoder[ByteArray] =
    Encoder.encodeString.contramap[ByteArray](ba =>
      Base64.getEncoder().encodeToString(ba.array)
    )

  implicit val timestampDecoder: Decoder[Timestamp] =
    Decoder.decodeLong.map(Timestamp.fromEpochSecond(_))

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

// object Play extends DefaultReads {
//   implicit val timestampDecoder: Reads[Timestamp] = {
//     LongReads.flatMap { epoch =>
//       val timestampStr =
//         schematic.Timestamp.fromEpochSecond(epoch).dateTime.toString()
//       val timestampOpt = Timestamp.parse(timestampStr).toOption

//       timestampOpt match {
//         case Some(t) => Reads.pure(t)
//         case None    => Reads.failed(s"Failed to decode as timestamp")
//       }
//     }
//   }

//   implicit val permissionsDecoder: Reads[Permission] =
//     Reads.apply[Permission] { json: JsValue =>
//       val read = (json \ "read").asOpt[Boolean]
//       val write = (json \ "write").asOpt[Boolean]
//       val directory = (json \ "directory").asOpt[Boolean]
//       JsSuccess.apply(Permission(read, write, directory))
//     }

//   implicit val metadataDecoder: Reads[Metadata] =
//     Reads.apply[Metadata] { json: JsValue =>
//       val contentType = (json \ "contentType").asOpt[String]
//       val lastModified = (json \ "lastModified").asOpt[Timestamp]
//       val checkSum = (json \ "checkSum").asOpt[String]
//       val pendingDeletion = (json \ "pendingDeletion").asOpt[Boolean]
//       val etag = (json \ "etag").asOpt[String]

//       JsSuccess.apply(
//         Metadata(
//           contentType,
//           lastModified,
//           checkSum,
//           pendingDeletion,
//           etag
//         )
//       )
//     }

//   implicit val credentialsDecoder: Reads[Creds] = {
//     Reads.apply[Creds] { json: JsValue =>
//       val user = (json \ "user").asOpt[String]
//       val key = (json \ "key").asOpt[String]

//       JsSuccess.apply(Creds(user, key))
//     }
//   }

//   implicit val encryptionMetadataDecoder: Reads[EncryptionMetadata] = {
//     Reads.apply[EncryptionMetadata] { json: JsValue =>
//       val system = (json \ "system").asOpt[String]
//       val credentials = (json \ "credentials").asOpt[Creds]
//       val partial = (json \ "partial").asOpt[Boolean]

//       JsSuccess.apply(EncryptionMetadata(system, credentials, partial))
//     }
//   }

//   implicit val encryptionDecoder: Reads[Encryption] = {
//     Reads.apply[Encryption] { json: JsValue =>
//       val user = (json \ "user").asOpt[String]
//       val date = (json \ "date").asOpt[Timestamp]
//       val metadata = (json \ "metadata").asOpt[EncryptionMetadata]

//       JsSuccess.apply(Encryption(user, date, metadata))
//     }
//   }

//   implicit val attributesDecoder: Reads[Attributes] = {
//     Reads.apply[Attributes] { json: JsValue =>
//       val user = (json \ "user").as[String]
//       val public = (json \ "public").as[Boolean]
//       val size = (json \ "size").as[Long]
//       val creationDate = (json \ "creationDate").as[Timestamp]
//       val region = (json \ "region").as[String]
//       val queryable = (json \ "queryable").asOpt[Boolean]
//       val queryableLastChange = (json \ "queryableLastChange").asOpt[Timestamp]
//       val blockPublicAccess = (json \ "blockPublicAccess").asOpt[Boolean]
//       val permissions = (json \ "permissions").asOpt[List[Permission]]
//       val tags = (json \ "tags").asOpt[List[String]]
//       val backedUp = (json \ "backedUp").asOpt[Boolean]
//       val metadata = (json \ "metadata").asOpt[List[Metadata]]
//       val encryption = (json \ "encryption").asOpt[Encryption]

//       JsSuccess.apply(
//         Attributes(
//           queryable = queryable,
//           queryableLastChange = queryableLastChange,
//           user = user,
//           public = public,
//           blockPublicAccess = blockPublicAccess,
//           permissions = permissions,
//           size = size,
//           tags = tags,
//           creationDate = creationDate,
//           backedUp = backedUp,
//           region = region,
//           metadata = metadata,
//           encryption = encryption
//         )
//       )
//     }
//   }

//   implicit val objectDecoder: Reads[S3Object] = {
//     Reads.apply[S3Object] { json: JsValue =>
//       val id = (json \ "id").as[String]
//       val owner = (json \ "owner").as[String]
//       val data = (json \ "data").as[String].map(_.toByte).toArray
//       val attributes = (json \ "attributes").as[Attributes]

//       JsSuccess.apply(
//         S3Object(
//           id = id,
//           owner = owner,
//           attributes = attributes,
//           data = ByteArray(data)
//         )
//       )
//     }
//   }
// }
