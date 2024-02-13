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

package smithy4s
package codecs

import smithy4s.capability.instances.either._
import smithy4s.schema.Primitive._

import schema._

object StringAndBlobCodecs {

  object decoders extends CachedSchemaCompiler.Optional.Impl[BlobDecoder] {
    def fromSchemaAux[A](
        schema: Schema[A],
        cache: AuxCache
    ): Option[BlobDecoder[A]] =
      StringAndBlobReaderVisitor(schema)
  }

  object encoders extends CachedSchemaCompiler.Optional.Impl[BlobEncoder] {
    def fromSchemaAux[A](
        schema: Schema[A],
        cache: AuxCache
    ): Option[BlobEncoder[A]] =
      StringAndBlobWriterVisitor(schema)
  }

  private type MaybeBlobDecoder[A] = Option[BlobDecoder[A]]
  private type MaybeBlobEncoder[A] = Option[BlobEncoder[A]]

  private object StringAndBlobReaderVisitor
      extends SchemaVisitor.Default[MaybeBlobDecoder] {
    self =>

    override def default[A]: MaybeBlobDecoder[A] = None

    override def primitive[P](
        shapeId: ShapeId,
        hints: Hints,
        tag: Primitive[P]
    ): MaybeBlobDecoder[P] = tag match {
      case PString => Some(stringDecoder)
      case PBlob   => Some(blobDecoder)
      case _       => None
    }

    override def enumeration[E](
        shapeId: ShapeId,
        hints: Hints,
        tag: EnumTag[E],
        values: List[EnumValue[E]]
    ): MaybeBlobDecoder[E] = {
      tag match {
        case EnumTag.StringEnum(_, None) =>
          Some(new BlobDecoder[E] {
            def decode(blob: Blob): Either[PayloadError, E] = {
              val str = blob.toUTF8String
              values.find(_.stringValue == str) match {
                case Some(enumValue) => Right(enumValue.value)
                case None =>
                  Left(
                    PayloadError(
                      PayloadPath.root,
                      s"expected one of ${values.mkString(",")}",
                      s"Unknown enum value $str"
                    )
                  )
              }
            }
          })
        case EnumTag.StringEnum(_, Some(processUnknown)) =>
          Some(new BlobDecoder[E] {
            def decode(blob: Blob): Either[PayloadError, E] = {
              val str = blob.toUTF8String
              val result: E = values
                .find(_.stringValue == str)
                .map(_.value)
                .getOrElse(processUnknown(str))
              Right(result)
            }
          })
        case _ => None
      }
    }

    override def biject[A, B](
        schema: Schema[A],
        bijection: Bijection[A, B]
    ): MaybeBlobDecoder[B] =
      self(schema).map(_.map(bijection.to))

    override def refine[A, B](
        schema: Schema[A],
        refinement: Refinement[A, B]
    ): MaybeBlobDecoder[B] =
      self(schema).map(decoderA =>
        new BlobDecoder[B] {
          def decode(blob: Blob): Either[PayloadError, B] =
            decoderA
              .decode(blob)
              .flatMap(refinement.asFunction(_).left.map { error =>
                PayloadError(
                  PayloadPath.root,
                  refinement.tag.id.show,
                  error.getMessage
                )
              })
        }
      )

    override def option[A](
        schema: Schema[A]
    ): MaybeBlobDecoder[Option[A]] =
      self(schema).map(decoderA =>
        new BlobDecoder[Option[A]] {
          def decode(blob: Blob): Either[PayloadError, Option[A]] =
            if (blob.isEmpty) Right(None)
            else decoderA.decode(blob).map(a => Some(a))
        }
      )
  }

  private object StringAndBlobWriterVisitor
      extends SchemaVisitor.Default[MaybeBlobEncoder] {
    self =>

    override def default[A]: MaybeBlobEncoder[A] = None

    override def primitive[P](
        shapeId: ShapeId,
        hints: Hints,
        tag: Primitive[P]
    ): MaybeBlobEncoder[P] = tag match {
      case PString => Some(stringEncoder)
      case PBlob   => Some(blobWriter)
      case _       => None
    }

    override def enumeration[E](
        shapeId: ShapeId,
        hints: Hints,
        tag: EnumTag[E],
        values: List[EnumValue[E]]
    ): MaybeBlobEncoder[E] = {
      tag match {
        case EnumTag.StringEnum(value, _) =>
          Some(stringEncoder.contramap(value))
        case _ => None
      }
    }

    override def biject[A, B](
        schema: Schema[A],
        bijection: Bijection[A, B]
    ): MaybeBlobEncoder[B] =
      self(schema).map(_.contramap(bijection.from))

    override def refine[A, B](
        schema: Schema[A],
        refinement: Refinement[A, B]
    ): MaybeBlobEncoder[B] =
      self(schema).map(_.contramap(refinement.from))

    override def option[A](
        schema: Schema[A]
    ): MaybeBlobEncoder[Option[A]] =
      self(schema).map(writerA =>
        new BlobEncoder[Option[A]] {
          def encode(maybeA: Option[A]): Blob = maybeA match {
            case Some(a) => writerA.encode(a)
            case None    => Blob.empty
          }
        }
      )
  }

  private val stringDecoder: BlobDecoder[String] = {
    new BlobDecoder[String] {
      def decode(blob: Blob): Either[PayloadError, String] = Right(
        blob.toUTF8String
      )
    }
  }

  private val stringEncoder: PayloadEncoder[String] =
    Encoder.lift(Blob(_))

  private val blobDecoder: BlobDecoder[Blob] = {
    new BlobDecoder[Blob] {
      def decode(blob: Blob): Either[PayloadError, Blob] = Right(
        blob
      )
    }
  }

  private val blobWriter: BlobEncoder[Blob] =
    Encoder.lift(identity)
}
