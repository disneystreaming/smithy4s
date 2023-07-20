/*
 *  Copyright 2021-2023 Disney Streaming
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

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import smithy4s.codecs.PayloadPath
import smithy4s.codecs.PayloadPath.Segment
import scala.collection.mutable
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.http.internals.UrlFormCursor
import smithy4s.http.internals.UrlFormDataEncoder
import smithy4s.http.internals.UrlFormDataEncoderSchemaVisitor
import smithy4s.http.internals.UrlFormDataDecoderSchemaVisitor

import smithy4s.schema.Schema
import Schema._
import smithy4s.ShapeTag
import smithy4s.ShapeId

final case class UrlForm(values: UrlForm.FormData.MultipleValues)

object UrlForm {

  case class Action(value: String)

  object Action extends ShapeTag.Companion[Action] {

    val id: ShapeId = ShapeId("smithy4s.http.UrlForm", "Action")

    val schema: Schema[Action] =
      string
        .biject[Action](
          Action(_),
          (_: Action).value
        )

  }

  case class Version(value: String)

  object Version extends ShapeTag.Companion[Version] {

    val id: ShapeId = ShapeId("smithy4s.http.UrlForm", "Version")

    val schema: Schema[Version] =
      string
        .biject[Version](
          Version(_),
          (_: Version).value
        )

  }

  // TODO: Clean this up
  val empty: UrlForm =
    UrlForm(values = UrlForm.FormData.MultipleValues(values = Vector.empty))

  sealed trait FormData extends Product with Serializable {
    def render: String
    def prepend(segment: PayloadPath.Segment): FormData
    def toPathedValues: Vector[FormData.PathedValue]
    def widen: FormData = this
    def down(segment: PayloadPath.Segment): FormData
  }
  object FormData {
    case object Empty extends FormData {
      override def render: String = ""

      override def prepend(segment: PayloadPath.Segment): FormData = this

      override def toPathedValues: Vector[FormData.PathedValue] = Vector.empty

      // TODO: Should this be an error?
      override def down(segment: PayloadPath.Segment): FormData = this
    }

    // TODO: Rename as Value, replace uses by PathedValue?
    final case class SimpleValue(str: String) extends FormData {
      // TODO: Use Codec, here and elsewhere, like UrlFormParser
      override def render: String =
        URLEncoder.encode(str, StandardCharsets.UTF_8.name())

      override def prepend(segment: PayloadPath.Segment): PathedValue =
        PathedValue(PayloadPath(segment), str)

      override def toPathedValues: Vector[FormData.PathedValue] = Vector.empty

      // TODO: Should this be an error?
      override def down(segment: PayloadPath.Segment): FormData = this
    }
    // TODO: Make value an option?
    final case class PathedValue(path: PayloadPath, value: String)
        extends FormData {

      override def render: String = {
        val lastIndex = path.segments.size - 1
        val key = path.segments.zipWithIndex
          .foldLeft(new mutable.StringBuilder) {

            case (builder, (Segment.Label(label), i)) if i < lastIndex =>
              builder.append(
                URLEncoder.encode(label, StandardCharsets.UTF_8.name())
              )
              builder.append('.')

            case (builder, (Segment.Index(index), i)) if i < lastIndex =>
              builder.append(index)
              builder.append('.')

            case (builder, (Segment.Label(label), _)) =>
              builder.append(
                URLEncoder.encode(label, StandardCharsets.UTF_8.name())
              )

            case (builder, (Segment.Index(index), _)) =>
              builder.append(index)
          }
          .toString()

        key + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8.name())
      }

      override def prepend(segment: PayloadPath.Segment): PathedValue =
        copy(path.prepend(segment), value)

      override def toPathedValues: Vector[FormData.PathedValue] = Vector(this)

      override def down(segment: PayloadPath.Segment): FormData =
        if (path.segments.head == segment)
          PathedValue(PayloadPath(path.segments.tail), value)
        // TODO: Should this be an error?
        else Empty
    }

    // TODO: Rename as Values?
    final case class MultipleValues(values: Vector[PathedValue])
        extends FormData {

      override def render: String =
        values.map(_.render).filter(str => str.nonEmpty).mkString("&")
      override def prepend(segment: PayloadPath.Segment): MultipleValues =
        copy(values.map(_.prepend(segment)))

      override def toPathedValues: Vector[FormData.PathedValue] = values

      override def down(segment: PayloadPath.Segment): FormData = {
        // TODO: Should we error if any are dropped? Probably not.
        val newValues = values.map(_.down(segment)).collect {
          case pathedValue: FormData.PathedValue => pathedValue
        }
        MultipleValues(newValues)
      }
    }
  }

  trait Decoder[A] {
    def decode(urlForm: UrlForm): Either[UrlFormDecodeError, A]
  }

  object Decoder extends CachedSchemaCompiler.Impl[Decoder] {
    def fromSchema[A](schema: Schema[A], cache: Cache): Decoder[A] =
      new Decoder[A] {
        val urlFormDataDecoder = UrlFormDataDecoderSchemaVisitor(schema)
        def decode(urlForm: UrlForm): Either[UrlFormDecodeError, A] =
          urlFormDataDecoder.decode(UrlFormCursor.fromUrlForm(urlForm))
      }
  }

  trait Encoder[A] {
    def encode(a: A): UrlForm
  }
  object Encoder extends CachedSchemaCompiler.Impl[Encoder] {
    protected type Aux[A] = UrlFormDataEncoder[A]
    def fromSchema[A](schema: Schema[A], cache: Cache): Encoder[A] = {
      val urlFormDataEncoder = new UrlFormDataEncoderSchemaVisitor(cache)(
        schema
      )
      new Encoder[A] {
        def encode(value: A): UrlForm = {
          val formData = urlFormDataEncoder.encode(value)
          // TODO: It smells a bit to have this in UrlForm which is otherwise mostly free of AWS concerns...
          val maybeAction = schema.hints.get[UrlForm.Action]
          val maybeVersion = schema.hints.get[UrlForm.Version]
          UrlForm(
            UrlForm.FormData.MultipleValues(
              maybeAction
                .map(action =>
                  UrlForm.FormData.PathedValue(
                    PayloadPath(PayloadPath.Segment("Action")),
                    action.value
                  )
                )
                .toVector ++
                maybeVersion
                  .map(version =>
                    UrlForm.FormData.PathedValue(
                      PayloadPath(PayloadPath.Segment("Version")),
                      version.value
                    )
                  )
                  .toVector ++
                formData.toPathedValues
            )
          )
        }
      }
    }
  }

}
