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

package smithy4s.http

import smithy4s.codecs.Writer
import smithy4s.kinds.PolyFunction
import smithy4s.schema.CachedSchemaCompiler

final case class HttpResponse[A](
    statusCode: Int,
    headers: Map[CaseInsensitive, Seq[String]],
    body: A
) {
  def withStatusCode(statusCode: Int): HttpResponse[A] =
    this.copy(statusCode = statusCode)
  def withHeaders(headers: Map[CaseInsensitive, Seq[String]]): HttpResponse[A] =
    this.copy(headers = headers)
  def addHeaders(headers: Map[CaseInsensitive, Seq[String]]): HttpResponse[A] =
    this.copy(headers = this.headers ++ headers)
}

object HttpResponse {
  type Encoder[Body, A] =
    Writer[HttpResponse[Body], HttpResponse[Body], A]
  type BodyEncoder[Body, A] = Writer[Any, Body, A]

  private def isStatusCodeSuccess(statusCode: Int): Boolean =
    100 <= statusCode && statusCode <= 399

  def fromMetadataEncoder[Body]: Encoder[Body, Metadata] = {
    (resp: HttpResponse[Body], meta: Metadata) =>
      val statusCode =
        meta.statusCode
          .filter(isStatusCodeSuccess)
          .getOrElse(resp.statusCode)
      resp
        .withStatusCode(statusCode)
        .addHeaders(meta.headers)
  }

  def fromMetadataEncoderK[Body]
      : PolyFunction[Metadata.Encoder, Encoder[Body, *]] =
    new PolyFunction[Metadata.Encoder, Encoder[Body, *]]() {

      override def apply[A](
          fa: Metadata.Encoder[A]
      ): Writer[HttpResponse[Body], HttpResponse[Body], A] =
        fromMetadataEncoder.contramap(fa.encode)
    }

  def fromEntityEncoderK[Body]
      : PolyFunction[BodyEncoder[Body, *], Encoder[Body, *]] =
    new PolyFunction[BodyEncoder[Body, *], Encoder[Body, *]]() {

      override def apply[A](
          fa: BodyEncoder[Body, A]
      ): Writer[HttpResponse[Body], HttpResponse[Body], A] =
        new Writer[HttpResponse[Body], HttpResponse[Body], A] {
          override def write(
              input: HttpResponse[Body],
              a: A
          ): HttpResponse[Body] =
            input.copy(body = fa.encode(a))
        }
    }

  def restSchemaCompiler[Body](
      metadataEncoderCompiler: CachedSchemaCompiler[Metadata.Encoder],
      entityEncoderCompiler: CachedSchemaCompiler[BodyEncoder[Body, *]]
  ): CachedSchemaCompiler[Encoder[Body, *]] = {
    val metadataCompiler = metadataEncoderCompiler.mapK(
      fromMetadataEncoderK[Body]
    )
    val entityCompiler =
      entityEncoderCompiler.mapK(fromEntityEncoderK[Body])
    HttpRestSchema.combineWriterCompilers(metadataCompiler, entityCompiler)
  }
}
