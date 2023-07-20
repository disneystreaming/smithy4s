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
package internals

import smithy.api.{XmlFlattened, XmlName}
import smithy4s.http._
import smithy4s.schema._
import smithy4s.codecs.PayloadPath

// TODO: Caching
object UrlFormDataEncoderSchemaVisitor
    extends SchemaVisitor[UrlFormDataEncoder] {
  compile =>

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): UrlFormDataEncoder[P] = new UrlFormDataEncoder[P] {
    override def encode(p: P): UrlForm.FormData =
      Primitive.stringWriter(tag, hints) match {
        case Some(writer) => UrlForm.FormData.SimpleValue(writer(p)).widen
        case None         => UrlForm.FormData.Empty.widen
      }
  }

  private val SkipEmpty = Hints.Binding.DynamicBinding(
    ShapeId("smithy4s.http.internals", "SkipEmpty"),
    Document.DNull
  )

  override def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): UrlFormDataEncoder[C[A]] = {
    val memberWriter = compile(member)
    val maybeKey =
      if (hints.has[XmlFlattened]) None
      else Option(getKey(member.hints, "member"))
    val skipEmpty = hints.toMap.contains(SkipEmpty.keyId)

    new UrlFormDataEncoder[C[A]] {
      override def encode(collection: C[A]): UrlForm.FormData = {
        val formData = UrlForm.FormData
          .MultipleValues(
            tag
              .iterator(collection)
              .zipWithIndex
              .flatMap { case (a, index) =>
                memberWriter
                  .encode(a)
                  .prepend(PayloadPath.Segment(index + 1))
                  .toPathedValues
              }
              .toVector
          )
          .widen
        if (tag.isEmpty(collection) && !skipEmpty) {
          val emptyValue = UrlForm.FormData.PathedValue(PayloadPath.root, "")
          UrlForm.FormData.MultipleValues(Vector(emptyValue))
        } else {
          maybeKey.fold(formData)(key => formData.prepend(key))
        }
      }
    }
  }

  override def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): UrlFormDataEncoder[Map[K, V]] = {
    type KV = (K, V)
    val kvSchema: Schema[(K, V)] = {
      val kField = key.required[KV]("key", _._1)
      val vField = value.required[KV]("value", _._2)
      Schema.struct(kField, vField)((_, _)).addHints(XmlName("entry"))
    }
    // avoid serialising empty maps, see https://github.com/smithy-lang/smithy/issues/1868
    val schema = Schema.vector(kvSchema).addHints(hints).addHints(SkipEmpty)
    val encoder = compile(schema)

    new UrlFormDataEncoder[Map[K, V]] {
      override def encode(m: Map[K, V]): UrlForm.FormData =
        encoder.encode(m.toVector)
    }
  }

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      tag: EnumTag,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): UrlFormDataEncoder[E] =
    tag match {
      case EnumTag.IntEnum =>
        new UrlFormDataEncoder[E] {
          override def encode(value: E): UrlForm.FormData =
            UrlForm.FormData.SimpleValue(total(value).intValue.toString)
        }

      case EnumTag.StringEnum =>
        new UrlFormDataEncoder[E] {
          override def encode(value: E): UrlForm.FormData =
            UrlForm.FormData.SimpleValue(total(value).stringValue)
        }
    }

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[Field[S, _]],
      make: IndexedSeq[Any] => S
  ): UrlFormDataEncoder[S] = {
    def fieldEncoder[A](field: Field[S, A]): UrlFormDataEncoder[S] = {
      val fieldKey = getKey(field.hints, field.label)
      compile(field.schema).contramap(field.get).prepend(fieldKey)
    }

    val codecs: Vector[UrlFormDataEncoder[S]] =
      fields.map(field => fieldEncoder(field))

    new UrlFormDataEncoder[S] {
      override def encode(s: S): UrlForm.FormData = {
        UrlForm.FormData.MultipleValues(
          codecs.flatMap(_.encode(s).toPathedValues)
        )
      }
    }
  }

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[Alt[U, _]],
      dispatch: Alt.Dispatcher[U]
  ): UrlFormDataEncoder[U] = {

    def encodeUnion[A](u: U, alt: Alt[U, A]): UrlForm.FormData = {
      val key = getKey(alt.hints, alt.label)
      dispatch
        .projector(alt)(u)
        .fold(UrlForm.FormData.Empty.widen)(a => compile(alt.schema).encode(a))
        .prepend(key)
    }

    new UrlFormDataEncoder[U] {
      override def encode(u: U): UrlForm.FormData =
        UrlForm.FormData.MultipleValues(
          alternatives.flatMap(encodeUnion(u, _).toPathedValues)
        )
    }
  }

  override def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): UrlFormDataEncoder[B] = new UrlFormDataEncoder[B] {
    override def encode(b: B): UrlForm.FormData =
      compile(schema).encode(bijection.from(b))
  }

  override def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): UrlFormDataEncoder[B] = new UrlFormDataEncoder[B] {
    override def encode(b: B): UrlForm.FormData =
      compile(schema).encode(refinement.from(b))
  }

  override def lazily[A](suspend: Lazy[Schema[A]]): UrlFormDataEncoder[A] =
    new UrlFormDataEncoder[A] {
      lazy val underlying: UrlFormDataEncoder[A] =
        suspend.map(schema => compile(schema)).value
      override def encode(a: A): UrlForm.FormData = underlying.encode(a)
    }

  override def option[A](schema: Schema[A]): UrlFormDataEncoder[Option[A]] =
    new UrlFormDataEncoder[Option[A]] {
      val encoder = compile(schema)
      override def encode(value: Option[A]): UrlForm.FormData = value match {
        case None        => UrlForm.FormData.Empty
        case Some(value) => encoder.encode(value)
      }
    }

  private def getKey(hints: Hints, default: String): PayloadPath.Segment =
    hints
      .get(XmlName)
      .map(_.value)
      .map(PayloadPath.Segment(_))
      .getOrElse(default)

}
