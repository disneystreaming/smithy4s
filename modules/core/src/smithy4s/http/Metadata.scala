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

import smithy4s.http.internals.MetaEncode._
import smithy4s.http.internals.SchemaVisitorMetadataWriter
import smithy4s.http.internals.SchemaVisitorMetadataReader
import scala.collection.mutable.{Map => MMap}

/**
  * Datatype containing metadata associated to a http message.
  *
  * The metadata is what is found in the http headers, and can be
  * derived from the http path, the query parameters, or the headers.
  *
  * Associated to it are a pair of Encoder/Decoder typeclasses, that
  * can be derived from a schema.
  *
  * @param path the path parameters of the http message
  * @param query the query parameters of the http message
  * @param headers the header parameters of the http message
  */
case class Metadata(
    path: Map[String, String] = Map.empty,
    query: Map[String, Seq[String]] = Map.empty,
    headers: Map[CaseInsensitive, Seq[String]] = Map.empty
) { self =>

  def headersFlattened: Vector[(CaseInsensitive, String)] =
    headers.toVector.flatMap { case (k, v) =>
      v.map(k -> _)
    }

  def queryFlattened: Vector[(String, String)] = query.toVector.flatMap {
    case (k, v) => v.map(k -> _)
  }

  def addHeader(ciKey: CaseInsensitive, value: String): Metadata = {
    headers.get(ciKey) match {
      case Some(existing) =>
        copy(headers = headers + (ciKey -> (value +: existing)))
      case None =>
        copy(headers = headers + (ciKey -> List(value)))
    }
  }
  def addHeader(str: String, value: String): Metadata = {
    addHeader(CaseInsensitive(str), value)
  }
  def addPathParam(key: String, value: String): Metadata =
    copy(path = path + (key -> value))
  def addQueryParam(key: String, value: String): Metadata =
    query.get(key) match {
      case Some(existing) =>
        copy(query = query + (key -> (value +: existing)))
      case None => copy(query = query + (key -> List(value)))
    }
  def addMultipleHeaders(
      ciKey: CaseInsensitive,
      value: List[String]
  ): Metadata = {
    headers.get(ciKey) match {
      case Some(existing) =>
        copy(headers = headers + (ciKey -> (value ++ existing)))
      case None => copy(headers = headers + (ciKey -> value))
    }
  }
  def addMultipleHeaders(key: String, value: List[String]): Metadata =
    addMultipleHeaders(CaseInsensitive(key), value)

  def addMultipleQueryParams(key: String, value: List[String]): Metadata =
    query.get(key) match {
      case Some(existing) =>
        copy(query = query + (key -> (value ++ existing)))
      case None => copy(query = query + (key -> value))
    }

  def ++(other: Metadata): Metadata = {
    def mergeMaps[K, V](
        left: Map[K, Seq[V]],
        right: Map[K, Seq[V]]
    ): Map[K, Seq[V]] = {
      val m = for {
        (k, v) <- left
        (k2, v2) <- right if k == k2
      } yield (k, v ++ v2)
      val l = left.filterNot { case (k, _) => right.contains(k) }
      val r = right.filterNot { case (k, _) => left.contains(k) }
      l ++ r ++ m
    }

    Metadata(
      this.path ++ other.path,
      mergeMaps(this.query, other.query),
      mergeMaps(this.headers, other.headers)
    )
  }

  def find(location: HttpBinding): Option[(String, List[String])] =
    location match {
      case HttpBinding.HeaderBinding(httpName) =>
        headers.get(httpName).flatMap {
          case head :: tl => Some((head, tl))
          case Nil        => None
        }
      case HttpBinding.QueryBinding(httpName) =>
        query.get(httpName).flatMap {
          case head :: tl => Some((head, tl))
          case Nil        => None
        }
      case HttpBinding.PathBinding(httpName) => path.get(httpName).map(_ -> Nil)
      case _                                 => None
    }
}

object Metadata {
  def fold[A](i: Iterable[A])(f: A => Metadata): Metadata =
    i.foldLeft(empty)((acc, a) => acc ++ f(a))

  val empty = Metadata(Map.empty, Map.empty, Map.empty)

  trait Access {
    def metadata: Metadata
  }

  def encode[A](a: A)(implicit encoder: Encoder[A]): Metadata =
    encoder.encode(a)

  /**
    * Decodes all the fields that are bound to http metadata,
    * shoving values in a map.
    */
  def decodePartial[A](metadata: Metadata)(implicit
      decoder: PartialDecoder[A]
  ): Either[MetadataError, MetadataPartial[A]] =
    decoder.decode(metadata)

  /**
    * If possible, attempts to decode the whole data from http metadata.
    * This will only return a non-empty value when all fields of the datatype
    * are bound to http metadata (ie path parameters, headers, query)
    *
    * @return None when the value cannot be decoded just from metadata
    */
  def decodeTotal[A](metadata: Metadata)(implicit
      decoder: PartialDecoder[A]
  ): Option[Either[MetadataError, A]] =
    decoder.total.map(_.decode(metadata))

  /**
    * Reads metadata and produces a map that contains values extracted from it, labelled
    * by field names.
    */
  trait PartialDecoder[A] {
    def decode(metadata: Metadata): Either[MetadataError, MetadataPartial[A]]

    def total: Option[TotalDecoder[A]]
  }

  object PartialDecoder {

    def apply[A](implicit instance: PartialDecoder[A]): PartialDecoder[A] =
      instance

    def fromSchema[A](schema: Schema[A]): PartialDecoder[A] = {
      val metaDecode = SchemaVisitorMetadataReader(schema)
      val (partial, maybeTotal) =
        metaDecode match {
          case internals.MetaDecode.StructureMetaDecode(partial, maybeTotal) =>
            (partial, maybeTotal)
          case _ => ((_: Metadata) => Right(MMap.empty[String, Any]), None)
        }

      new PartialDecoder[A] {
        def decode(
            metadata: Metadata
        ): Either[MetadataError, MetadataPartial[A]] =
          partial(metadata).map(MetadataPartial(_))

        def total: Option[TotalDecoder[A]] =
          maybeTotal.map(total =>
            new TotalDecoder[A] {
              def decode(metadata: Metadata): Either[MetadataError, A] =
                total(metadata)
            }
          )
      }
    }

    implicit def derivedDecoderFromStaticSchema[A](implicit
        schema: Schema[A]
    ): PartialDecoder[A] = decoderCache(schema)

    private val decoderCache =
      new PolyFunction[Schema, PartialDecoder] {
        def apply[A](fa: Schema[A]): PartialDecoder[A] = fromSchema(fa)
      }.unsafeMemoise
  }

  /**
    * Reads metadata and produces a value. Cannot be produced implicitly from a schema,
    * as the required information is not available at the typelevel.
    */
  trait TotalDecoder[A] {
    def decode(metadata: Metadata): Either[MetadataError, A]
  }

  object TotalDecoder {
    def apply[A](implicit instance: TotalDecoder[A]): TotalDecoder[A] =
      instance

    def fromSchema[A](schema: Schema[A]): Option[TotalDecoder[A]] = {
      val metaDecode = SchemaVisitorMetadataReader(schema)
      metaDecode match {
        case internals.MetaDecode.StructureMetaDecode(_, maybeTotal) =>
          maybeTotal.map { total => (metadata: Metadata) => total(metadata) }
        case _ => None
      }
    }
  }

  trait Encoder[A] {
    def encode(a: A): Metadata
  }

  object Encoder {

    def apply[A](implicit instance: Encoder[A]): Encoder[A] = instance

    def fromSchema[A](schema: Schema[A]): Encoder[A] = {
      SchemaVisitorMetadataWriter(schema) match {
        case StructureMetaEncode(f) => (a: A) => f(a)
        case _                      => (_: A) => Metadata.empty
      }
    }

    implicit def deriveEncoderFromStaticSchema[A](implicit
        schema: Schema[A]
    ): Encoder[A] = encoderCache(schema)

    private val encoderCache =
      new PolyFunction[smithy4s.Schema, Encoder] {
        def apply[A](fa: smithy4s.Schema[A]): Encoder[A] = fromSchema(fa)
      }.unsafeMemoise

  }

}
