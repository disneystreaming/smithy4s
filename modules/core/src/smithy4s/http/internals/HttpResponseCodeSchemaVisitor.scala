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

package smithy4s.http.internals

import smithy4s.capability.Contravariant
import smithy4s.Hints
import smithy4s.schema.{
  EnumTag,
  EnumValue,
  Field,
  Primitive,
  Schema,
  SchemaField,
  SchemaVisitor
}
import smithy4s.ShapeId
import smithy4s.http.internals.HttpResponseCodeSchemaVisitor.{
  NoResponseCode,
  OptionalResponseCode,
  RequiredResponseCode,
  ResponseCodeExtractor
}

class HttpResponseCodeSchemaVisitor()
    extends SchemaVisitor.DefaultIgnoringInput[ResponseCodeExtractor] {
  def default[A]: ResponseCodeExtractor[A] = NoResponseCode
  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): ResponseCodeExtractor[P] = tag match {
    case Primitive.PInt =>
      if (hints.has[smithy.api.HttpResponseCode]) {
        HttpResponseCodeSchemaVisitor.int
      } else {
        NoResponseCode
      }
    case _ => NoResponseCode
  }

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      tag: EnumTag,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): ResponseCodeExtractor[E] =
    tag match {
      case EnumTag.IntEnum if hints.has[smithy.api.HttpResponseCode] =>
        Contravariant[ResponseCodeExtractor].contramap(
          HttpResponseCodeSchemaVisitor.int
        )(e => total(e).intValue)
      case _ =>
        NoResponseCode
    }

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): ResponseCodeExtractor[S] = {
    def compileField[A](
        field: SchemaField[S, A]
    ): Option[ResponseCodeExtractor[S]] = {
      val folder = new Field.Folder[Schema, S, ResponseCodeExtractor[S]]() {
        def onOptional[AA](
            label: String,
            instance: Schema[AA],
            get: S => Option[AA]
        ): ResponseCodeExtractor[S] = {
          val aExt = apply(instance)
          OptionalResponseCode { s =>
            get(s) match {
              case None => None
              case Some(value) =>
                aExt match {
                  case NoResponseCode          => None
                  case RequiredResponseCode(f) => Some(f(value))
                  case OptionalResponseCode(f) => f(value)
                }
            }
          }
        }
        def onRequired[AA](
            label: String,
            instance: Schema[AA],
            get: S => AA
        ): ResponseCodeExtractor[S] = {
          val aExt = apply(instance)
          Contravariant[ResponseCodeExtractor].contramap(aExt)(get)
        }
      }

      field.hints
        .get[smithy.api.HttpResponseCode]
        .map(_ => field.fold(folder))
    }
    fields.flatMap(f => compileField(f)).headOption.getOrElse(NoResponseCode)
  }

}

object HttpResponseCodeSchemaVisitor {
  sealed trait ResponseCodeExtractor[-A]
  object NoResponseCode extends ResponseCodeExtractor[Any]
  case class RequiredResponseCode[A](f: A => Int)
      extends ResponseCodeExtractor[A]
  case class OptionalResponseCode[A](f: A => Option[Int])
      extends ResponseCodeExtractor[A]

  implicit val contravariant: Contravariant[ResponseCodeExtractor] =
    new Contravariant[ResponseCodeExtractor]() {
      def contramap[A, B](
          fa: ResponseCodeExtractor[A]
      )(f: B => A): ResponseCodeExtractor[B] = {
        fa match {
          case NoResponseCode          => NoResponseCode
          case RequiredResponseCode(g) => RequiredResponseCode { b => g(f(b)) }
          case OptionalResponseCode(g) => OptionalResponseCode { b => g(f(b)) }
        }
      }
    }

  val int: ResponseCodeExtractor[Int] = {
    RequiredResponseCode(identity)
  }
}
