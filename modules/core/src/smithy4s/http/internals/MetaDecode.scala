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
package internals

import smithy4s.capability.Covariant
import smithy4s.http.internals.MetaDecode._

import scala.collection.mutable.{Map => MMap}

import HttpBinding._

private[http] sealed abstract class MetaDecode[+A] {
  def map[B](to: A => B): MetaDecode[B] = this match {
    case StringValueMetaDecode(f) => StringValueMetaDecode(f andThen to)
    case StringCollectionMetaDecode(f) =>
      StringCollectionMetaDecode(f andThen to)
    case StringMapMetaDecode(f)     => StringMapMetaDecode(f andThen to)
    case StringListMapMetaDecode(f) => StringListMapMetaDecode(f andThen to)
    case EmptyMetaDecode            => EmptyMetaDecode
    case StructureMetaDecode(f, maybeTotal) => {
      val newTotal = maybeTotal.map { total => (m: Metadata) =>
        total(m).map(to)
      }
      StructureMetaDecode(f, newTotal)
    }
  }

  private[internals] def updateMetadata(
      binding: HttpBinding,
      fieldName: String,
      optional: Boolean,
      maybeDefault: Option[Any]
  ): (Metadata, PutField) => Unit = {
    // format: off

    // Looks up metadata for the specific key, then applies some procedure.
    // Throws an exception if the field optional is unset and the key is not
    // found.
    def lookupAndProcess[K, T] (m: Metadata => Map[K, T], key: K)
                  (process: (T, String, PutFieldCallback) => Unit) :
                  (Metadata, PutField) => Unit =  {
      (metadata, putField) =>
        m(metadata).get(key) match {
          case Some(value) if optional => process(value, fieldName, putField.putSome(_, _))
          case Some(value)             => process(value, fieldName, putField.putRequired(_, _))
          case None if maybeDefault.isDefined => putField.putRequired(fieldName, maybeDefault.get)
          case None if optional        => putField.putNone(fieldName)
          case None => throw new MetadataError.NotFound(fieldName, binding)
        }
    }
    // format: on

    (binding, this) match {
      case (PathBinding(path), StringValueMetaDecode(f)) =>
        lookupAndProcess(_.path, path) { (value, fieldName, putField) =>
          putField(fieldName, f(value))
        }
      case (HeaderBinding(h), StringValueMetaDecode(f)) =>
        lookupAndProcess(_.headers, h) { (values, fieldName, putField) =>
          if (values.size == 1) {
            putField(fieldName, f(values.head))
          } else throw MetadataError.ArityError(fieldName, binding)
        }
      case (HeaderBinding(h), StringCollectionMetaDecode(f)) =>
        lookupAndProcess(_.headers, h) { (values, fieldName, putField) =>
          putField(fieldName, f(values.iterator))
        }
      case (QueryBinding(h), StringValueMetaDecode(f)) =>
        lookupAndProcess(_.query, h) { (values, fieldName, putField) =>
          if (values.size == 1) {
            putField(fieldName, f(values.head))
          } else throw MetadataError.ArityError(fieldName, binding)
        }
      case (QueryBinding(q), StringCollectionMetaDecode(f)) =>
        lookupAndProcess(_.query, q) { (values, fieldName, putField) =>
          putField(fieldName, f(values.iterator))
        }
      // see https://smithy.io/2.0/spec/http-bindings.html#httpqueryparams-trait
      // when targeting Map[String,String] we take the first value encountered
      case (QueryParamsBinding, StringMapMetaDecode(f)) => {
        (metadata, putField) =>
          val iter: Iterator[(FieldName, FieldName)] = metadata.query.iterator
            .map { case (k, values) =>
              k -> values.head
            }
          if (iter.nonEmpty && optional) putField.putSome(fieldName, f(iter))
          else if (iter.isEmpty && optional) putField.putNone(fieldName)
          else putField.putRequired(fieldName, f(iter))
      }
      case (QueryParamsBinding, StringListMapMetaDecode(f)) => {
        (metadata, putField) =>
          val iter = metadata.query.iterator
            .map { case (k, values) =>
              k -> values.iterator
            }
          if (iter.nonEmpty && optional) putField.putSome(fieldName, f(iter))
          else if (iter.isEmpty && optional) putField.putNone(fieldName)
          else putField.putRequired(fieldName, f(iter))
      }
      case (HeaderPrefixBinding(prefix), StringMapMetaDecode(f)) => {
        (metadata, putField) =>
          val iter = metadata.headers.iterator
            .collect {
              case (k, values) if k.startsWith(prefix) =>
                if (values.size == 1) {
                  k.toString.drop(prefix.length()) -> values.head
                } else
                  throw MetadataError.ArityError(fieldName, HeaderBinding(k))
            }
          if (iter.nonEmpty && optional) putField.putSome(fieldName, f(iter))
          else if (iter.isEmpty && optional) putField.putNone(fieldName)
          else putField.putRequired(fieldName, f(iter))
      }
      case (HeaderPrefixBinding(prefix), StringListMapMetaDecode(f)) => {
        (metadata, putField) =>
          val iter = metadata.headers.iterator
            .collect {
              case (k, values) if k.startsWith(prefix) =>
                k.toString.drop(prefix.length()) -> values.iterator
            }
          if (iter.nonEmpty && optional) putField.putSome(fieldName, f(iter))
          else if (iter.isEmpty && optional) putField.putNone(fieldName)
          else putField.putRequired(fieldName, f(iter))
      }
      case (StatusCodeBinding, _) =>
        (metadata, putField) =>
          metadata.statusCode match {
            case None =>
              if (optional) putField.putNone(fieldName)
              else
                sys.error(
                  "Status code is not available and required field needs it."
                )
            case Some(value) =>
              if (optional) putField.putSome(fieldName, value)
              else putField.putRequired(fieldName, value)
          }
      case _ => (metadata: Metadata, buffer) => ()
    }
  }

}

private[http] object MetaDecode {

  type FieldName = String
  case class MetaDecodeError(f: (String, HttpBinding) => MetadataError)
      extends Throwable
      with scala.util.control.NoStackTrace

  // format: off
  final case class StringValueMetaDecode[A](f: String => A) extends MetaDecode[A]
  final case class StringCollectionMetaDecode[A](f: Iterator[String] => A) extends MetaDecode[A]
  final case class StringMapMetaDecode[A](f: Iterator[(String, String)] => A) extends MetaDecode[A]
  final case class StringListMapMetaDecode[A](f: Iterator[(String, Iterator[String])] => A) extends MetaDecode[A]
  case object EmptyMetaDecode extends MetaDecode[Nothing]
  final case class StructureMetaDecode[A](f: Metadata => Either[MetadataError, MMap[String, Any]], total: Option[Metadata => Either[MetadataError, A]]) extends MetaDecode[A]
  // format: on

  type PutFieldCallback = (String, Any) => Unit

  trait PutField {
    def putRequired(fieldName: String, value: Any): Unit
    def putSome(fieldName: String, value: Any): Unit
    def putNone(fieldName: String): Unit
  }

  implicit val covariantInstance: Covariant[MetaDecode] =
    new Covariant[MetaDecode] {
      def map[A, B](fa: MetaDecode[A])(f: A => B): MetaDecode[B] =
        fa.map(f)
      def emap[A, B](
          fa: MetaDecode[A]
      )(f: A => Either[ConstraintError, B]): MetaDecode[B] =
        fa.map(a =>
          f(a) match {
            case Right(b) => b
            case Left(error) =>
              throw MetaDecodeError(
                MetadataError.FailedConstraint(_, _, error.message)
              )
          }
        )
    }

  def fromUnsafe[A](
      expectedType: String
  )(f: String => A): MetaDecode[A] =
    StringValueMetaDecode[A] { string =>
      try { f(string) }
      catch {
        case _: Throwable =>
          throw MetaDecodeError(
            MetadataError.WrongType(
              _,
              _,
              expectedType,
              string
            )
          )
      }
    }

  def from[A](
      expectedType: String
  )(f: String => Option[A]): MetaDecode[A] =
    StringValueMetaDecode[A] { string =>
      f(string) match {
        case Some(value) =>
          value
        case None =>
          throw MetaDecodeError(
            MetadataError.WrongType(
              _,
              _,
              expectedType,
              string
            )
          )
      }

    }

}
