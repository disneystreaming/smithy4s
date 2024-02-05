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
package http

import smithy4s.http.internals.HttpResponseCodeSchemaVisitor
import smithy4s.http.internals.MetaEncode._
import smithy4s.http.internals.SchemaVisitorMetadataReader
import smithy4s.http.internals.SchemaVisitorMetadataWriter
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.schema.CompilationCache

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
  * @param statusCode the int value of a http response status code
  */
case class Metadata private (
    path: Map[String, String],
    query: Map[String, Seq[String]],
    headers: Map[CaseInsensitive, Seq[String]],
    statusCode: Option[Int]
) { self =>

  def withPath(value: Map[String, String]): Metadata = {
    copy(path = value)
  }

  def withQuery(value: Map[String, Seq[String]]): Metadata = {
    copy(query = value)
  }

  def withHeaders(value: Map[CaseInsensitive, Seq[String]]): Metadata = {
    copy(headers = value)
  }

  def withStatusCode(value: Option[Int]): Metadata = {
    copy(statusCode = value)
  }
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
        copy(query = query + (key -> (existing :+ value)))
      case None => copy(query = query + (key -> List(value)))
    }
  def addQueryParamsIfNoExist(key: String, values: String*): Metadata =
    query.get(key) match {
      case Some(_) => self
      case None    => copy(query = query + (key -> values.toList))
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
        copy(query = query + (key -> (existing ++ value)))
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
      mergeMaps(this.headers, other.headers),
      this.statusCode.orElse(other.statusCode)
    )
  }

  def find(location: HttpBinding): Option[(String, List[String])] =
    location match {
      case hb: HttpBinding.HeaderBinding =>
        headers.get(hb.httpName).flatMap {
          case head :: tl => Some((head, tl))
          case Nil        => None
        }
      case qb: HttpBinding.QueryBinding =>
        query.get(qb.httpName).flatMap {
          case head :: tl => Some((head, tl))
          case Nil        => None
        }
      case pb: HttpBinding.PathBinding => path.get(pb.httpName).map(_ -> Nil)
      case _                           => None
    }
}

object Metadata {
  @scala.annotation.nowarn(
    "msg=private method unapply in object Metadata is never used"
  )
  private def unapply(c: Metadata): Option[Metadata] = Some(c)
  def apply(
      path: Map[String, String] = Map.empty,
      query: Map[String, Seq[String]] = Map.empty,
      headers: Map[CaseInsensitive, Seq[String]] = Map.empty,
      statusCode: Option[Int] = None
  ): Metadata = {
    new Metadata(path, query, headers, statusCode)
  }
  def fold[A](i: Iterable[A])(f: A => Metadata): Metadata =
    i.foldLeft(empty)((acc, a) => acc ++ f(a))

  val empty = Metadata(Map.empty, Map.empty, Map.empty, None)

  trait Access {
    def metadata: Metadata
  }

  def encode[A](a: A)(implicit encoder: Encoder[A]): Metadata =
    encoder.encode(a)

  implicit def encoderFromSchema[A: Schema]: Encoder[A] =
    Encoder.derivedImplicitInstance

  /**
    * If possible, decode the data from fields that are bound to http metadata.
    */
  def decode[A](metadata: Metadata)(implicit
      decoder: Decoder[A]
  ): Either[MetadataError, A] =
    decoder.decode(metadata)

  /**
    * Reads metadata and produces a map that contains values extracted from it, labelled
    * by field names.
    */
  type Decoder[A] =
    smithy4s.codecs.Decoder[Either[MetadataError, *], Metadata, A]

  implicit def decoderFromSchema[A: Schema]: Decoder[A] =
    Decoder.derivedImplicitInstance

  object Decoder extends CachedDecoderCompilerImpl(awsHeaderEncoding = false) {
    type Compiler = CachedSchemaCompiler[Decoder]
  }

  private[smithy4s] object AwsDecoder
      extends CachedDecoderCompilerImpl(awsHeaderEncoding = true)

  private[http] class CachedDecoderCompilerImpl(awsHeaderEncoding: Boolean)
      extends CachedSchemaCompiler.DerivingImpl[Decoder] {
    type Aux[A] = internals.MetaDecode[A]

    def apply[A](implicit instance: Decoder[A]): Decoder[A] =
      instance

    def fromSchema[A](
        schema: Schema[A],
        cache: CompilationCache[internals.MetaDecode]
    ): Decoder[A] = {
      val metaDecode =
        new SchemaVisitorMetadataReader(cache, awsHeaderEncoding)(schema)
      metaDecode match {
        case internals.MetaDecode.StructureMetaDecode(decodeFunction) =>
          decodeFunction(_: Metadata)
        case _ =>
          (_: Metadata) =>
            Left(
              MetadataError.ImpossibleDecoding(
                "Impossible to formulate a decoder for the data"
              )
            )
      }
    }
  }

  /**
    * Reads metadata and produces a map that contains values extracted from it, labelled
    * by field names.
    */
  type Encoder[A] = smithy4s.codecs.Encoder[Metadata, A]

  trait EncoderCompiler extends CachedSchemaCompiler[Metadata.Encoder] {
    def withExplicitDefaultsEncoding(explicitDefaults: Boolean): EncoderCompiler
  }

  object Encoder
      extends CachedEncoderCompilerImpl(
        awsHeaderEncoding = false,
        explicitDefaultsEncoding = false
      ) {
    type Compiler = CachedSchemaCompiler[Encoder]
  }

  private[smithy4s] object AwsEncoder
      extends CachedEncoderCompilerImpl(
        awsHeaderEncoding = true,
        explicitDefaultsEncoding = false
      )

  private[http] class CachedEncoderCompilerImpl(
      awsHeaderEncoding: Boolean,
      explicitDefaultsEncoding: Boolean
  ) extends CachedSchemaCompiler.DerivingImpl[Encoder]
      with EncoderCompiler {

    type Aux[A] = internals.MetaEncode[A]

    def apply[A](implicit instance: Encoder[A]): Encoder[A] = instance

    def withExplicitDefaultsEncoding(
        explicitDefaultsEncoding: Boolean
    ): EncoderCompiler = new CachedEncoderCompilerImpl(
      awsHeaderEncoding = awsHeaderEncoding,
      explicitDefaultsEncoding = explicitDefaultsEncoding
    )

    def fromSchema[A](
        schema: Schema[A],
        cache: Cache
    ): Encoder[A] = {
      val toStatusCode: A => Option[Int] = { a =>
        schema.compile(new HttpResponseCodeSchemaVisitor()) match {
          case HttpResponseCodeSchemaVisitor.NoResponseCode =>
            None
          case HttpResponseCodeSchemaVisitor.RequiredResponseCode(ext) =>
            Some(ext(a))
          case HttpResponseCodeSchemaVisitor.OptionalResponseCode(ext) =>
            ext(a)
        }
      }
      val schemaVisitor = new SchemaVisitorMetadataWriter(
        cache,
        commaDelimitedEncoding = awsHeaderEncoding,
        explicitDefaultsEncoding = explicitDefaultsEncoding
      )
      schemaVisitor(schema) match {
        case StructureMetaEncode(f) if awsHeaderEncoding => { (a: A) =>
          val metadata = f(a)
          // AWS does not want to receive empty header values.
          val trimmedHeaders = metadata.headers
            .map { case (key, values) => (key, values.filterNot(_.isEmpty)) }
            .filterNot(_._2.isEmpty)
            .toMap
          metadata.copy(statusCode = toStatusCode(a), headers = trimmedHeaders)
        }
        case StructureMetaEncode(f) => { (a: A) =>
          val struct = f(a)
          struct.copy(statusCode = toStatusCode(a))
        }
        case _ => (_: A) => Metadata.empty
      }
    }
  }

}
