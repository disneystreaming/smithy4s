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

package smithy4s
package http

import smithy4s.PartialData
import smithy.api.HttpPayload
import smithy4s.schema._
import smithy4s.schema.SchemaPartition.NoMatch
import smithy4s.schema.SchemaPartition.SplittingMatch
import smithy4s.schema.SchemaPartition.TotalMatch
import smithy4s.codecs.Writer
import smithy4s.codecs.Reader
import smithy4s.capability.Zipper

/**
 * This construct indicates how a schema is split between http metadata
 * (ie headers, path parameters, query parameters, status code) and body.
 *
 * When the input or the output of an http operation has some elements that
 * are coming from the body and some elements that are coming from the metadata,
 * the schema is split in two schemas that each track the relevant subset.
 *
 * The partial data resulting from the successful decoding of both subsets can
 * be reconciled to recover the total data.
 *
 * On the encoding side, the split allows to only encode the relevant subset of
 * data as http headers, and the other subset as http body.
 */
sealed trait HttpRestSchema[A]

object HttpRestSchema {

  // format: off
  final case class OnlyMetadata[A](schema: Schema[A]) extends HttpRestSchema[A]
  final case class OnlyBody[A](schema: Schema[A]) extends HttpRestSchema[A]
  final case class MetadataAndBody[A](metadataSchema: Schema[PartialData[A]], bodySchema: Schema[PartialData[A]]) extends HttpRestSchema[A]
  final case class Empty[A](value: A) extends HttpRestSchema[A]
  // format: on

  def apply[A](fullSchema: Schema[A]): HttpRestSchema[A] = {

    def isMetadataField(field: SchemaField[_, _]): Boolean = HttpBinding
      .fromHints(field.label, field.instance.hints, fullSchema.hints)
      .isDefined

    def isPayloadField(field: SchemaField[_, _]): Boolean =
      field.instance.hints.has[HttpPayload]

    fullSchema.findPayload(isPayloadField) match {
      case TotalMatch(schema) => OnlyBody(schema)
      case NoMatch() =>
        fullSchema.partition(isMetadataField) match {
          case SplittingMatch(metadataSchema, bodySchema) =>
            MetadataAndBody(metadataSchema, bodySchema)
          case TotalMatch(schema) =>
            OnlyMetadata(schema)
          case NoMatch() =>
            fullSchema match {
              case Schema.StructSchema(_, _, fields, make)
                  if (fields.isEmpty) =>
                Empty(make(IndexedSeq.empty))
              case _ => OnlyBody(fullSchema)
            }
        }
      case SplittingMatch(bodySchema, metadataSchema) =>
        MetadataAndBody(metadataSchema, bodySchema)
    }
  }

  /**
    * Combines separate compilers :
    *  - one specific to http metadata
    *  - one specific to http bodies
    *
    * the result is a compiler that knows how to split schemas so that upon
    * encoding a piece of data, the relevant subset of the data is encoded as
    * http metadata (headers, query parameters, etc) and the relevant subset
    * of the data is encoded as http body.
    */
  def combineWriterCompilers[Message](
      metadataEncoderCompiler: Writer.CachedCompiler[Message, Message],
      bodyEncoderCompiler: Writer.CachedCompiler[Message, Message]
  ): Writer.CachedCompiler[Message, Message] =
    new Writer.CachedCompiler[Message, Message] {

      type MetadataCache = metadataEncoderCompiler.Cache
      type BodyCache = bodyEncoderCompiler.Cache
      type Cache = (MetadataCache, BodyCache)
      def createCache(): Cache = {
        val mCache = metadataEncoderCompiler.createCache()
        val bCache = bodyEncoderCompiler.createCache()
        (mCache, bCache)
      }
      def fromSchema[A](schema: Schema[A]): Writer[Message, Message, A] =
        fromSchema(schema, createCache())

      def fromSchema[A](
          fullSchema: Schema[A],
          cache: Cache
      ): Writer[Message, Message, A] = {
        HttpRestSchema(fullSchema) match {
          case HttpRestSchema.OnlyMetadata(metadataSchema) =>
            // The data can be fully decoded from the metadata.
            metadataEncoderCompiler.fromSchema(metadataSchema, cache._1)
          case HttpRestSchema.OnlyBody(bodySchema) =>
            // The data can be fully decoded from the body
            bodyEncoderCompiler.fromSchema(bodySchema, cache._2)
          case HttpRestSchema.MetadataAndBody(metadataSchema, bodySchema) =>
            val metadataEncoder =
              metadataEncoderCompiler
                .fromSchema(metadataSchema, cache._1)
                .contramap[A](PartialData.Total(_))
            val bodyEncoder =
              bodyEncoderCompiler
                .fromSchema(bodySchema, cache._2)
                .contramap[A](PartialData.Total(_))
            // The order matters here, as the metadata encoder might override headers
            // that would be set with body encoders (if a smithy member is annotated with
            // `@httpHeader("Content-Type")` for instance)
            bodyEncoder.pipe(metadataEncoder)
          case HttpRestSchema.Empty(_) =>
            Writer.noop
          // format: on
        }
      }
    }

  /**
    * A compiler for Reader that abides by REST-semantics :
    * fields that are annotated with `httpLabel`, `httpHeader`, `httpQuery`,
    * `httpStatusCode` ... are decoded from the corresponding metadata.
    *
    * The rest is decoded from the body.
    */
  // scalafmt: {maxColumn = 120}
  def combineReaderCompilers[F[_]: Zipper, Message](
      metadataDecoderCompiler: CachedSchemaCompiler[Reader[F, Message, *]],
      bodyDecoderCompiler: CachedSchemaCompiler[Reader[F, Message, *]]
  ): CachedSchemaCompiler[Reader[F, Message, *]] =
    new CachedSchemaCompiler[Reader[F, Message, *]] {
      val zipper = Zipper[Reader[F, Message, *]]

      type MetadataCache = metadataDecoderCompiler.Cache
      type BodyCache = bodyDecoderCompiler.Cache
      type Cache = (MetadataCache, BodyCache)
      def createCache(): Cache = {
        val mCache = metadataDecoderCompiler.createCache()
        val bCache = bodyDecoderCompiler.createCache()
        (mCache, bCache)
      }
      def fromSchema[A](schema: Schema[A]) =
        fromSchema(schema, createCache())

      def fromSchema[A](fullSchema: Schema[A], cache: Cache) = {
        HttpRestSchema(fullSchema) match {
          case HttpRestSchema.OnlyMetadata(metadataSchema) =>
            // The data can be fully decoded from the metadata,
            // but we still decoding Unit from the body to drain the message.
            val metadataDecoder =
              metadataDecoderCompiler.fromSchema(metadataSchema, cache._1)
            val bodyDecoder =
              bodyDecoderCompiler.fromSchema(Schema.unit, cache._2)
            zipper.zipMap(bodyDecoder, metadataDecoder) { case (_, data) => data }
          case HttpRestSchema.OnlyBody(bodySchema) =>
            // The data can be fully decoded from the body
            bodyDecoderCompiler.fromSchema(bodySchema, cache._2)
          case HttpRestSchema.MetadataAndBody(metadataSchema, bodySchema) =>
            val metadataDecoder: Reader[F, Message, PartialData[A]] =
              metadataDecoderCompiler.fromSchema(metadataSchema, cache._1)
            val bodyDecoder: Reader[F, Message, PartialData[A]] =
              bodyDecoderCompiler.fromSchema(bodySchema, cache._2)
            zipper.zipMap(metadataDecoder, bodyDecoder)(
              PartialData.unsafeReconcile(_, _)
            )
          case HttpRestSchema.Empty(value) =>
            zipper.pure(value)
          // format: on
        }
      }
    }

}
