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

package smithy4s.aws.json

import smithy4s.HintMask
import smithy4s.http.json.JCodec
import smithy4s.http.HttpMediaType

private[aws] class AwsJsonCodecAPI(
    httpMediaType: HttpMediaType.Type = HttpMediaType("application/json"),
    hintMask: Option[HintMask] = None
) extends smithy4s.http.json.JsonCodecAPI(
      new AwsSchemaVisitorJCodec(_),
      hintMask
    ) {

  override def writeToArray[A](codec: Codec[A], value: A): Array[Byte] = {
    // AWS expects an empty object to be sent by clients even when the data
    // that is meant to be a payload is empty (for instance, in case of an optional
    // `@httpPayload` member)
    val result = super.writeToArray(codec, value)
    if (result.isEmpty) "{}".getBytes() else result
  }

  override def mediaType[A](codec: JCodec[A]): HttpMediaType.Type =
    httpMediaType

}
