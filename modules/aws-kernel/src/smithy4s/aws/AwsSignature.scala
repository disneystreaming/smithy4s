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

package smithy4s.aws.kernel

import smithy4s.http.internals.URIEncoderDecoder.{encodeOthers => uriEncode}
import smithy4s.http.CaseInsensitive
import smithy4s.http.HttpMethod

/**
  * Implementation of the AWS Signature Version 4 algorithm.
  *
  * See https://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-header-based-auth.html
  *
  * The [[baguette.CryptoPlatformCompat]] contains all the platform-specific
  * code that has to do with hashing/encrypting, and is provided for the
  * three platforms (jvm/js/native.)
  */
private[aws] object AwsSignature {

  private[aws] val algorithm = "AWS4-HMAC-SHA256"

  import AwsCrypto._

  private[aws] trait Signable {
    def httpMethod: HttpMethod
    def httpPath: String
    def queryParams: Seq[(String, String)]
    // sorted headers without the auth
    def baseHeaders: Seq[(CaseInsensitive, String)]
    // sorted, semicolumn-separated, lowercase header names
    def signedHeadersString: String
    // whether a header should be signed (should be consistent with signedHeadersString)
    def isSigned(header: String): Boolean
    def body: Option[Array[Byte]]
    def secretKey: String
    def timestamp: Timestamp
    def region: String
    def endpointPrefix: String

    def credentialsScope =
      s"${timestamp.conciseDate}/$region/${endpointPrefix.toLowerCase}/aws4_request"

    def canonicalQueryString =
      if (queryParams.isEmpty) ""
      else
        queryParams
          .map { case (k, v) =>
            uriEncode(k) + "=" + uriEncode(v)
          }
          .mkString("&")

    def canonicalHeadersString =
      baseHeaders
        .collect {
          case (key, value) if isSigned(key.toString) =>
            key.toString.toLowerCase + ":" + value.trim
        }
        .mkString("\n")
  }

  def apply(request: Signable): String = {

    val payloadHash = sha256HexDigest(
      request.body.getOrElse(Array.emptyByteArray)
    )

    val canonicalRequest = List[String](
      request.httpMethod.showUppercase,
      request.httpPath,
      request.canonicalQueryString,
      request.canonicalHeadersString,
      "",
      request.signedHeadersString,
      payloadHash
    ).mkString("\n")
    val canonicalRequestHash = sha256HexDigest(canonicalRequest)
    val signatureKey = getSignatureKey(
      request.secretKey,
      request.timestamp.conciseDate,
      request.region,
      request.endpointPrefix.toLowerCase
    )
    val stringToSign = List[String](
      algorithm,
      request.timestamp.conciseDateTime,
      request.credentialsScope,
      canonicalRequestHash
    ).mkString("\n")
    toHexString(hmacSha256(stringToSign, signatureKey))
  }

  def getSignatureKey(
      key: String,
      dateStamp: String,
      regionName: String,
      serviceName: String
  ): Binary = {
    val kSecret = binaryFromString("AWS4" + key)
    val kDate = hmacSha256(dateStamp, kSecret)
    val kRegion = hmacSha256(regionName, kDate)
    val kService = hmacSha256(serviceName, kRegion)
    val kSigning = hmacSha256("aws4_request", kService)
    kSigning
  }

}
