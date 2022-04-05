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

package smithy4s
package http
package internals

import smithy4s.http.internals.MetaEncode._
import smithy4s.internals.Hinted
import smithy4s.internals.InputOutput
import smithy4s.schema._

import java.util.Base64
import java.util.UUID

import HttpBinding._

/**
  * This schematic works on data that is annotated with :
  * - smithy.api.httpLabel
  * - smithy.api.httpHeader
  * - smithy.api.httpPrefixHeaders
  * - smithy.api.httpQuery
  * - smithy.api.httpQueryParams
  *
  * As such, assumptions are made using the information of what types can be
  * annotated in the smithy specs.
  *
  */
private[http] object SchematicMetadataWriter
    extends Schematic[MetaEncode.Make] {

  def short: MetaEncode.Make[Short] = MetaEncode.Make.fromToString

  def int: MetaEncode.Make[Int] = MetaEncode.Make.fromToString

  def long: MetaEncode.Make[Long] = MetaEncode.Make.fromToString

  def double: MetaEncode.Make[Double] = MetaEncode.Make.fromToString

  def float: MetaEncode.Make[Float] = MetaEncode.Make.fromToString

  def bigint: MetaEncode.Make[BigInt] = MetaEncode.Make.fromToString

  def bigdecimal: MetaEncode.Make[BigDecimal] =
    MetaEncode.Make.fromToString

  def string: MetaEncode.Make[String] =
    MetaEncode.Make.stringValue(identity)

  def boolean: MetaEncode.Make[Boolean] = MetaEncode.Make.fromToString

  def uuid: MetaEncode.Make[UUID] = MetaEncode.Make.fromToString

  def byte: MetaEncode.Make[Byte] = MetaEncode.Make.fromToString

  def bytes: MetaEncode.Make[ByteArray] =
    MetaEncode.Make.stringValue(ba =>
      Base64.getEncoder().encodeToString(ba.array)
    )

  def timestamp: MetaEncode.Make[Timestamp] = Hinted[MetaEncode].from {
    (hints: Hints) =>
      (
        hints.get(HttpBinding).map(_.tpe),
        hints.get(smithy.api.TimestampFormat)
      ) match {
        case (_, Some(timestampFormat)) =>
          StringValueMetaEncode((timestamp: Timestamp) =>
            timestamp.format(timestampFormat)
          )
        case (Some(HttpBinding.Type.QueryType), None) |
            (Some(HttpBinding.Type.PathType), None) =>
          // See https://awslabs.github.io/smithy/1.0/spec/core/http-traits.html?highlight=httpquery#httpquery-trait
          StringValueMetaEncode((timestamp: Timestamp) =>
            timestamp.format(smithy.api.TimestampFormat.DATE_TIME)
          )
        case (Some(HttpBinding.Type.HeaderType), None) =>
          // See https://awslabs.github.io/smithy/1.0/spec/core/http-traits.html?highlight=httpquery#httpheader-trait
          StringValueMetaEncode((timestamp: Timestamp) =>
            timestamp.format(smithy.api.TimestampFormat.HTTP_DATE)
          )
        case _ => MetaEncode.empty
      }
  }

  def withHints[A](fa: Make[A], hints: Hints): Make[A] = fa.addHints(hints)

  def unit: MetaEncode.Make[Unit] = MetaEncode.Make.empty

  def list[S](fs: MetaEncode.Make[S]): MetaEncode.Make[List[S]] =
    fs.transform[List[S]] {
      case StringValueMetaEncode(f) =>
        StringListMetaEncode[List[S]](listS => listS.map(f))
      case _ => MetaEncode.empty
    }

  def document: Make[Document] = Make.empty

  def set[S](fs: MetaEncode.Make[S]): MetaEncode.Make[Set[S]] =
    fs.transform[Set[S]] {
      case StringValueMetaEncode(f) =>
        StringListMetaEncode[Set[S]](set => set.map(f).toList)
      case _ => MetaEncode.empty
    }

  def vector[S](fs: MetaEncode.Make[S]): MetaEncode.Make[Vector[S]] =
    fs.transform[Vector[S]] {
      case StringValueMetaEncode(f) =>
        StringListMetaEncode[Vector[S]](vector => vector.map(f).toList)
      case _ => MetaEncode.empty
    }

  def map[K, V](
      fk: MetaEncode.Make[K],
      fv: MetaEncode.Make[V]
  ): MetaEncode.Make[Map[K, V]] =
    fk.productTransform(fv) {
      case (
            StringValueMetaEncode(writeK),
            StringValueMetaEncode(writeV)
          ) =>
        StringMapMetaEncode[Map[K, V]](map =>
          map.map { case (k, v) => (writeK(k), writeV(v)) }.toMap
        )
      case (
            StringValueMetaEncode(writeK),
            StringListMetaEncode(writeLV)
          ) =>
        StringListMapMetaEncode[Map[K, V]](map =>
          map.map { case (k, v) => (writeK(k), writeLV(v)) }.toMap
        )
      case _ => MetaEncode.empty
    }

  def union[S](
      first: Alt[MetaEncode.Make, S, _],
      rest: Vector[Alt[MetaEncode.Make, S, _]]
  )(
      total: S => Alt.WithValue[MetaEncode.Make, S, _]
  ): MetaEncode.Make[S] = MetaEncode.Make.empty

  def enumeration[A](
      to: A => (String, Int),
      fromName: Map[String, A],
      fromOrdinal: Map[Int, A]
  ): MetaEncode.Make[A] = MetaEncode.Make.stringValue(to.andThen(_._1))

  def suspend[A](f: Lazy[MetaEncode.Make[A]]): MetaEncode.Make[A] =
    MetaEncode.Make.empty

  def bijection[A, B](
      f: MetaEncode.Make[A],
      to: A => B,
      from: B => A
  ): MetaEncode.Make[B] =
    f.contramap(from)

  def surjection[A, B](
      f: MetaEncode.Make[A],
      tags: List[ShapeTag[_]],
      to: A => Either[ConstraintError, B],
      from: B => A
  ): MetaEncode.Make[B] =
    f.contramap(from)

  def struct[S](
      fields: Vector[Field[MetaEncode.Make, S, _]]
  )(f: Vector[Any] => S): MetaEncode.Make[S] =
    Hinted[MetaEncode].onHintOpt[InputOutput, S] { maybeInputOutput =>
      def encodeField[A](
          field: Field[MetaEncode.Make, S, A]
      ): Option[(Metadata, S) => Metadata] = {
        val hints = field.instance.hints
        HttpBinding
          .fromHints(field.label, hints, maybeInputOutput)
          .map { binding =>
            val folderT = new Field.LeftFolder[MetaEncode.Make, Metadata] {
              def compile[T](
                  label: String,
                  instance: Make[T]
              ): (Metadata, T) => Metadata = {
                val encoder: MetaEncode[T] =
                  instance.addHints(Hints(binding) ++ hints).get
                val updateFunction = encoder.updateMetadata(binding)
                (metadata, t: T) => updateFunction(metadata, t)
              }
            }
            field.leftFolder(folderT)
          }
      }

      val updateFunctions =
        fields.map(f => encodeField(f)).collect { case Some(updateFunction) =>
          updateFunction
        }
      StructureMetaEncode(s =>
        updateFunctions.foldLeft(Metadata.empty)((metadata, updateFunction) =>
          updateFunction(metadata, s)
        )
      )
    }
}
