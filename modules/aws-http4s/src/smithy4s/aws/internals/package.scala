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

package smithy4s.aws

import fs2.compression.Compression
import smithy4s.Hints
import smithy4s.http.HttpRequest
import org.http4s.Entity
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.capability.MonadThrowLike
import smithy4s.http._
import smithy4s.http4s.kernel.{EntityWriter, EntityReader}
import smithy4s.codecs.Reader
import smithy4s.capability.Covariant
import smithy4s.interopcats._
import cats.effect.Concurrent
import smithy4s.xml.Xml
import fs2.Stream
import fs2.Pure

package object internals {

  private[internals] type RequestEncoderCompiler[F[_]] =
    CachedSchemaCompiler[HttpRequest.Encoder[Entity[F], *]]

  private[internals] def applyCompression[F[_]: Compression](
      hints: Hints,
      retainUserEncoding: Boolean = true
  ): RequestEncoderCompiler[F] => RequestEncoderCompiler[F] = {
    val compression =
      smithy4s.http4s.kernel.GzipRequestCompression[F](retainUserEncoding)
    import smithy4s.codecs.Writer
    hints.get(smithy.api.RequestCompression) match {
      case Some(rc) if rc.encodings.contains("gzip") =>
        (encoder: RequestEncoderCompiler[F]) =>
          encoder.mapK(Writer.andThenK_(compression))
      case _ => identity[RequestEncoderCompiler[F]]
    }
  }

  private[internals] def stringAndBlobRequestWriters[F[_]]
      : CachedSchemaCompiler.Optional[HttpRequest.Encoder[Entity[F], *]] =
    smithy4s.http.StringAndBlobCodecs.WriterCompiler.mapK {
      Covariant.liftPolyFunction[Option](
        HttpMediaTyped
          .liftPolyFunction(EntityWriter.fromPayloadWriterK[F])
          .andThen(HttpRequest.Encoder.fromHttpMediaWriterK)
      )
    }

  private[internals] def stringAndBlobResponseReaders[F[_]: Concurrent]
      : CachedSchemaCompiler.Optional[Reader[F, Entity[F], *]] =
    smithy4s.http.StringAndBlobCodecs.ReaderCompiler.mapK {
      Covariant.liftPolyFunction[Option](
        HttpMediaTyped
          .unwrappedK[HttpPayloadReader]
          .andThen(EntityReader.fromHttpPayloadReaderK[F])
      )
    }

  private[internals] def xmlRequestWriters[F[_]]
      : CachedSchemaCompiler[HttpRequest.Encoder[Entity[F], *]] =
    Xml.xmlByteStreamEncoders[fs2.Pure].mapK {
      smithy4s.codecs.Writer
        .addingTo[Any]
        .andThenK { (stream: Stream[Pure, Byte]) =>
          val bytes = stream.compile.to(fs2.Chunk)
          Entity(Stream.chunk[F, Byte](bytes), Some(bytes.size.toLong))
        }
        .andThen(HttpRequest.Encoder.fromEntityEncoderK("application/xml"))
    }

  private[internals] def xmlResponseReaders[F[_]: Concurrent] =
    Xml
      .xmlByteStreamDecoders[F]
      .mapK {
        Reader
          .liftPolyFunction(MonadThrowLike.mapErrorK[F](fromXmlToHttpError))
          .andThen(Reader.in[F].composeK((_: Entity[F]).body))
          .andThen(HttpResponse.extractBody[F, Entity[F]])
      }

  private val fromXmlToHttpError: PartialFunction[Throwable, Throwable] = {
    case xmlDecodeError: smithy4s.xml.XmlDecodeError =>
      smithy4s.http.HttpPayloadError(
        xmlDecodeError.path.toPayloadPath,
        "",
        xmlDecodeError.message
      )
  }

}
