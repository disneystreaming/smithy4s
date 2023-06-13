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

package smithy4s.http4s.kernel

import cats.effect.Concurrent
import org.http4s.EntityEncoder
import org.http4s.Response
import org.http4s.Status
import smithy4s.Errorable
import smithy4s.PartialData
import smithy4s.capability.Encoder
import smithy4s.http.HttpStatusCode
import smithy4s.http._
import smithy4s.schema.Alt
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.schema.Schema

object ResponseEncoder {

  def forError[F[_], E](
      errorTypeHeader: String,
      maybeErrorable: Option[Errorable[E]],
      encoderCompiler: CachedSchemaCompiler[ResponseEncoder[F, *]]
  ): ResponseEncoder[F, E] = maybeErrorable match {
    case Some(errorable) =>
      forErrorAux(errorTypeHeader, errorable, encoderCompiler)
    case None => Encoder.noop
  }

  private def forErrorAux[F[_], E](
      errorTypeHeader: String,
      errorable: Errorable[E],
      encoderCompiler: CachedSchemaCompiler[ResponseEncoder[F, *]]
  ): ResponseEncoder[F, E] = {
    val errorUnionSchema = errorable.error
    val dispatcher =
      Alt.Dispatcher(errorUnionSchema.alternatives, errorUnionSchema.dispatch)
    val precompiler = new Alt.Precompiler[Schema, ResponseEncoder[F, *]] {
      def apply[Err](
          label: String,
          errorSchema: Schema[Err]
      ): ResponseEncoder[F, Err] = new ResponseEncoder[F, Err] {
        val errorEncoder =
          encoderCompiler.fromSchema(errorSchema, encoderCompiler.createCache())
        def encode(response: Response[F], err: Err): Response[F] = {
          val errorCode =
            HttpStatusCode.fromSchema(errorSchema).code(err, 500)
          val status =
            Status.fromInt(errorCode).getOrElse(Status.InternalServerError)
          val encodedResponse = errorEncoder.encode(response, err)
          encodedResponse
            .withStatus(status)
            .withHeaders(encodedResponse.headers.put(errorTypeHeader -> label))
        }
      }
    }
    dispatcher.compile(precompiler)
  }

  def fromMetadataEncoder[F[_]: Concurrent, A](
      metadataEncoder: Metadata.Encoder[A]
  ): ResponseEncoder[F, A] = new ResponseEncoder[F, A] {

    def encode(response: Response[F], a: A): Response[F] = {
      val metadata = metadataEncoder.encode(a)
      val headers = toHeaders(metadata.headers)
      val status = metadata.statusCode
        .flatMap(Status.fromInt(_).toOption)
        .filter(_.responseClass.isSuccess)
        .getOrElse(response.status)
      response.withHeaders(response.headers ++ headers).withStatus(status)
    }
  }

  def fromEntityEncoder[F[_]: Concurrent, A](implicit
      entityEncoder: EntityEncoder[F, A]
  ): ResponseEncoder[F, A] = new ResponseEncoder[F, A] {
    def encode(response: Response[F], a: A): Response[F] = {
      response.withEntity(a)
    }
  }

  def rpcSchemaCompiler[F[_]](
      entityEncoderCompiler: CachedSchemaCompiler[EntityEncoder[F, *]]
  )(implicit F: Concurrent[F]): CachedSchemaCompiler[ResponseEncoder[F, *]] =
    new CachedSchemaCompiler[ResponseEncoder[F, *]] {
      type Cache = entityEncoderCompiler.Cache
      def createCache(): Cache =
        entityEncoderCompiler.createCache()

      def fromSchema[A](
          schema: Schema[A],
          cache: Cache
      ): ResponseEncoder[F, A] =
        fromEntityEncoder(F, entityEncoderCompiler.fromSchema(schema, cache))
      def fromSchema[A](schema: Schema[A]): ResponseEncoder[F, A] =
        fromEntityEncoder(F, entityEncoderCompiler.fromSchema(schema))
    }

  /**
    * A compiler for ResponseEncoder that abides by REST-semantics :
    * fields that are annotated with `httpLabel`, `httpHeader`, `httpQuery`,
    * `httpStatusCode` ... are encoded as the corresponding metadata.
    *
    * The rest is used to formulate the body of the message.
    */
  def restSchemaCompiler[F[_]](
      entityEncoderCompiler: CachedSchemaCompiler[EntityEncoder[F, *]]
  )(implicit
      F: Concurrent[F]
  ): CachedSchemaCompiler[ResponseEncoder[F, *]] =
    new CachedSchemaCompiler[ResponseEncoder[F, *]] {
      type MetadataCache = Metadata.Encoder.Cache
      type EntityCache = entityEncoderCompiler.Cache
      type Cache = (EntityCache, MetadataCache)
      def createCache(): Cache = {
        val eCache = entityEncoderCompiler.createCache()
        val mCache = Metadata.Encoder.createCache()
        (eCache, mCache)
      }
      def fromSchema[A](schema: Schema[A]): ResponseEncoder[F, A] =
        fromSchema(schema, createCache())

      def fromSchema[A](
          fullSchema: Schema[A],
          cache: Cache
      ): ResponseEncoder[F, A] = {
        HttpRestSchema(fullSchema) match {
          case HttpRestSchema.OnlyMetadata(metadataSchema) =>
            // The data can be fully decoded from the metadata.
            val metadataEncoder =
              Metadata.Encoder.fromSchema(metadataSchema, cache._2)
            ResponseEncoder.fromMetadataEncoder(metadataEncoder)
          case HttpRestSchema.OnlyBody(bodySchema) =>
            // The data can be fully decoded from the body
            implicit val bodyEncoder: EntityEncoder[F, A] =
              entityEncoderCompiler.fromSchema(bodySchema, cache._1)
            ResponseEncoder.fromEntityEncoder(F, bodyEncoder)
          case HttpRestSchema.MetadataAndBody(metadataSchema, bodySchema) =>
            val metadataEncoder =
              Metadata.Encoder.fromSchema(metadataSchema, cache._2)
            val metadataResponseEncoder =
              ResponseEncoder
                .fromMetadataEncoder(metadataEncoder)
                .contramap[A](PartialData.Total(_))
            implicit val bodyEncoder: EntityEncoder[F, A] =
              entityEncoderCompiler
                .fromSchema(bodySchema, cache._1)
                .contramap[A](PartialData.Total(_))
            val bodyResponseEncoder =
              ResponseEncoder
                .fromEntityEncoder(F, bodyEncoder)
            metadataResponseEncoder.combine(bodyResponseEncoder)
          case HttpRestSchema.Empty(_) =>
            Encoder.noop
          // format: on
        }
      }
    }

}
