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
package aws.query

import smithy4s.schema._
import smithy.api.{XmlFlattened, XmlName}
import smithy4s.{Schema => _, _}

private[aws] class AwsSchemaVisitorAwsQueryCodec(
    val cache: CompilationCache[AwsQueryCodec]
) extends SchemaVisitor.Cached[AwsQueryCodec] { compile =>

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): AwsQueryCodec[P] = new AwsQueryCodec[P] {
    override def apply(p: P): FormData =
      Primitive.stringWriter(tag, hints) match {
        case Some(writer) => FormData.SimpleValue(writer(p)).widen
        case None         => FormData.Empty.widen
      }
  }

  override def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): AwsQueryCodec[C[A]] = {
    val memberWriter = compile(member)
    val maybeKey =
      if (hints.has[XmlFlattened]) None
      else Option(getKey(member.hints, "member"))

    new AwsQueryCodec[C[A]] {
      override def apply(collection: C[A]): FormData = {
        val formData = FormData
          .MultipleValues(
            tag
              .iterator(collection)
              .zipWithIndex
              .map { case (member, index) =>
                memberWriter(member).prepend(index + 1)
              }
              .toVector
          )
          .widen
        maybeKey.fold(formData)(key => formData.prepend(key))
      }
    }
  }

  override def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): AwsQueryCodec[Map[K, V]] = {
    type KV = (K, V)
    val kvSchema: Schema[(K, V)] = {
      val kField = key.required[KV]("key", _._1)
      val vField = value.required[KV]("value", _._2)
      Schema.struct(kField, vField)((_, _)).addHints(XmlName("entry"))
    }
    val schema = Schema.vector(kvSchema).addHints(hints)
    val codec = compile(schema)

    new AwsQueryCodec[Map[K, V]] {
      override def apply(m: Map[K, V]): FormData = codec(m.toVector)
    }
  }

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      tag: EnumTag,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): AwsQueryCodec[E] = {
    if (hints.has(IntEnum))
      new AwsQueryCodec[E] {
        def apply(value: E): FormData =
          FormData.SimpleValue(total(value).intValue.toString)
      }
    else
      new AwsQueryCodec[E] {
        def apply(value: E): FormData =
          FormData.SimpleValue(total(value).stringValue)
      }
  }

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): AwsQueryCodec[S] = {
    def fieldEncoder[A](field: SchemaField[S, A]): AwsQueryCodec[S] = {
      val fieldKey = getKey(field.hints, field.label)

      val encoder = field.foldK(new Field.FolderK[Schema, S, AwsQueryCodec] {
        override def onRequired[AA](
            label: String,
            instance: Schema[AA],
            get: S => AA
        ): AwsQueryCodec[AA] = {
          val schema = compile(instance)
          new AwsQueryCodec[AA] {
            def apply(a: AA): FormData = schema(a)
          }
        }

        override def onOptional[AA](
            label: String,
            instance: Schema[AA],
            get: S => Option[AA]
        ): AwsQueryCodec[Option[AA]] = {
          val schema = compile(instance)
          new AwsQueryCodec[Option[AA]] {
            override def apply(a: Option[AA]): FormData = a match {
              case Some(value) => schema(value)
              case None        => FormData.Empty
            }
          }
        }
      })

      new AwsQueryCodec[S] {
        def apply(s: S): FormData =
          encoder(field.get(s)).prepend(fieldKey)
      }
    }

    val codecs: Vector[AwsQueryCodec[S]] =
      fields.map(field => fieldEncoder(field))

    new AwsQueryCodec[S] {
      def apply(s: S): FormData =
        FormData.MultipleValues(codecs.map(codec => codec(s)))
    }
  }

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[U, _]],
      dispatch: Alt.Dispatcher[Schema, U]
  ): AwsQueryCodec[U] = {

    def encode[A](u: U, alt: SchemaAlt[U, A]): FormData = {
      val key = getKey(alt.hints, alt.label)
      dispatch
        .projector(alt)(u)
        .fold(FormData.Empty.widen)(a => compile(alt.instance)(a))
        .prepend(key)
    }

    new AwsQueryCodec[U] {
      def apply(u: U): FormData =
        FormData.MultipleValues(alternatives.map(alt => encode(u, alt)))
    }
  }

  override def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): AwsQueryCodec[B] = new AwsQueryCodec[B] {
    def apply(b: B): FormData = compile(schema)(bijection.from(b))
  }

  override def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): AwsQueryCodec[B] = new AwsQueryCodec[B] {
    def apply(b: B): FormData = compile(schema)(refinement.from(b))
  }

  override def lazily[A](shapeId: ShapeId, hints: Hints, suspend: Lazy[Schema[A]]): AwsQueryCodec[A] =
    new AwsQueryCodec[A] {
      lazy val underlying: AwsQueryCodec[A] =
        suspend.map(schema => compile(schema)).value
      override def apply(a: A): FormData = { underlying(a) }
    }

  /**
    * @todo Pay more attention to the AWS Query annotations
   */
  private def getKey(hints: Hints, default: String): String =
    hints
      .get(XmlName)
      .map(_.value)
      .getOrElse(default)
}
