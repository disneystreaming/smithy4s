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
package internals

import smithy4s.capability.Contravariant

import HttpBinding._
import MetaEncode._

// Metadata encoders can only be used to encode a subset of the metamodel.
// See https://awslabs.github.io/smithy/1.0/spec/core/http-traits.html for more information
sealed trait MetaEncode[-A] {

  private[internals] def updateMetadata(
      binding: HttpBinding
  ): (Metadata, A) => Metadata =
    (binding, this) match {
      case (pb: PathBinding, StringValueMetaEncode(f)) =>
        (metadata: Metadata, a: A) => metadata.addPathParam(pb.httpName, f(a))
      case (hb: HeaderBinding, StringValueMetaEncode(f)) =>
        (metadata: Metadata, a: A) => metadata.addHeader(hb.httpName, f(a))
      case (hb: HeaderBinding, StringListMetaEncode(f)) =>
        (metadata: Metadata, a: A) =>
          metadata.addMultipleHeaders(hb.httpName, f(a))
      case (qb: QueryBinding, StringValueMetaEncode(f)) =>
        (metadata: Metadata, a: A) => metadata.addQueryParam(qb.httpName, f(a))
      case (qb: QueryBinding, StringListMetaEncode(f)) =>
        (metadata: Metadata, a: A) =>
          metadata.addMultipleQueryParams(qb.httpName, f(a))
      case (QueryParamsBinding, StringMapMetaEncode(f)) =>
        (metadata: Metadata, a: A) =>
          f(a).foldLeft(metadata) { case (m, (k, v)) =>
            m.addQueryParamsIfNoExist(k, v)
          }
      case (QueryParamsBinding, StringListMapMetaEncode(f)) =>
        (metadata: Metadata, a: A) =>
          f(a).foldLeft(metadata) { case (m, (k, v)) =>
            m.addQueryParamsIfNoExist(k, v: _*)
          }
      case (hpb: HeaderPrefixBinding, StringMapMetaEncode(f)) =>
        (metadata: Metadata, a: A) =>
          f(a).foldLeft(metadata) { case (m, (k, v)) =>
            m.addHeader(hpb.prefix + k, v)
          }
      case (hpb: HeaderPrefixBinding, StringListMapMetaEncode(f)) =>
        (metadata: Metadata, a: A) =>
          f(a).foldLeft(metadata) { case (m, (k, v)) =>
            m.addMultipleHeaders(hpb.prefix + k, v)
          }
      case _ => (metadata: Metadata, _: A) => metadata
    }

  def contramap[B](from: B => A): MetaEncode[B] = this match {
    case StringValueMetaEncode(f)   => StringValueMetaEncode(from andThen f)
    case StringListMetaEncode(f)    => StringListMetaEncode(from andThen f)
    case StringMapMetaEncode(f)     => StringMapMetaEncode(from andThen f)
    case StringListMapMetaEncode(f) => StringListMapMetaEncode(from andThen f)
    case EmptyMetaEncode            => EmptyMetaEncode
    case StructureMetaEncode(f)     => StructureMetaEncode(from andThen f)
  }
}
object MetaEncode {

  implicit val contravariantInstance: Contravariant[MetaEncode] =
    new Contravariant[MetaEncode] {
      def contramap[A, B](fa: MetaEncode[A])(f: B => A): MetaEncode[B] =
        fa.contramap(f)
    }

  // format: off
  case class StringValueMetaEncode[A](f: A => String) extends MetaEncode[A]
  case class StringListMetaEncode[A](f: A => List[String]) extends MetaEncode[A]
  case class StringMapMetaEncode[A](f: A => Map[String, String]) extends MetaEncode[A]
  case class StringListMapMetaEncode[A](f: A => Map[String, List[String]]) extends MetaEncode[A]
  case object EmptyMetaEncode extends MetaEncode[Any]
  case class StructureMetaEncode[S](f: S => Metadata) extends MetaEncode[S]
  // format: on

  def fromToString[A]: MetaEncode[A] = StringValueMetaEncode(_.toString())
  def empty[A]: MetaEncode[A] = EmptyMetaEncode

}
