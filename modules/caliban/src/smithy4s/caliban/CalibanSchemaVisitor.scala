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

package smithy4s.caliban

import caliban.schema._
import smithy4s.Bijection
import smithy4s.ByteArray
import smithy4s.Document
import smithy4s.Hints
import smithy4s.schema.SchemaVisitor
import smithy4s.schema.Field
import smithy4s.schema.Primitive
import smithy4s.schema.Field.Wrapped
import smithy4s.ShapeId
import smithy4s.Timestamp
import smithy4s.{Refinement}
import smithy4s.schema.SchemaAlt
import smithy4s.schema.Alt
import caliban.introspection.adt.__Type
import smithy4s.schema
import smithy4s.schema.CollectionTag
import smithy4s.schema.EnumTag.StringEnum
import smithy4s.schema.EnumTag.IntEnum

// todo: caching
private object CalibanSchemaVisitor
    extends SchemaVisitor.Default[Schema[Any, *]] {
  // todo: remaining cases
  override def default[A]: Schema[Any, A] = sys.error("unsupported schema")

  override def biject[A, B](
      schema: smithy4s.Schema[A],
      bijection: Bijection[A, B]
  ): Schema[Any, B] = schema.compile(this).contramap(bijection.from)

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): Schema[Any, P] = {
    implicit val byteSchema: Schema[Any, Byte] =
      Schema.unitSchema.asInstanceOf[Schema[Any, Byte]] // TODO
    implicit val blobSchema: Schema[Any, ByteArray] =
      Schema.unitSchema.asInstanceOf[Schema[Any, ByteArray]] // TODO
    implicit val documentSchema: Schema[Any, Document] =
      Schema.unitSchema.asInstanceOf[Schema[Any, Document]] // TODO
    implicit val timestampSchema: Schema[Any, Timestamp] =
      Schema.unitSchema.asInstanceOf[Schema[Any, Timestamp]] // TODO

    Primitive.deriving[Schema[Any, *]].apply(tag)
  }

  private def field[S, A](
      f: Field[Schema[Any, *], S, A]
  )(implicit fa: FieldAttributes) = {
    val schema = f
      .instanceA(new Field.ToOptional[Schema[Any, *]] {

        override def apply[A0](
            fa: Schema[Any, A0]
        ): Wrapped[Schema[Any, *], Option, A0] = Schema.optionSchema(fa)

      })

    Schema.field(f.label)(f.get)(
      schema,
      fa
    )
  }

  override def refine[A, B](
      schema: smithy4s.Schema[A],
      refinement: Refinement[A, B]
  ): Schema[Any, B] =
    schema.compile(this).contramap(refinement.from)

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[Field[smithy4s.Schema, S, ?]],
      make: IndexedSeq[Any] => S
  ): Schema[Any, S] =
    Schema.obj(shapeId.name, None) { implicit fa =>
      fields
        .map(_.mapK(this))
        .map(field(_))
        .toList
    }
  override def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: schema.Schema[A]
  ): Schema[Any, C[A]] = tag match {
    case CollectionTag.ListTag => Schema.listSchema(member.compile(this))
    case CollectionTag.IndexedSeqTag =>
      Schema.seqSchema(member.compile(this)).contramap(identity(_))
    case CollectionTag.VectorTag => Schema.vectorSchema(member.compile(this))
    case CollectionTag.SetTag    => Schema.setSchema(member.compile(this))
  }

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[U, _]],
      dispatch: Alt.Dispatcher[smithy4s.Schema, U]
  ): Schema[Any, U] = {
    val self = this

    type Resolve[A] = A => Step[Any]

    val resolve0 =
      dispatch.compile(new Alt.Precompiler[smithy4s.Schema, Resolve] {
        override def apply[A](
            label: String,
            instance: smithy4s.Schema[A]
        ): Resolve[A] = {
          val underlying = instance.compile(self)
          a =>
            Step.ObjectStep(
              shapeId.name + label + "Case",
              Map(label -> underlying.resolve(a))
            )
        }
      })

    new Schema[Any, U] {
      override def resolve(value: U): Step[Any] = resolve0(value)

      override def toType(isInput: Boolean, isSubscription: Boolean): __Type =
        Types.makeUnion(
          name = Some(shapeId.name),
          description = None,
          subTypes = alternatives
            .map(handleAlt(shapeId, _))
            .map(_.toType_(isInput, isSubscription))
            .toList
        )
    }
  }
  private def handleAlt[U, A](parent: ShapeId, alt: SchemaAlt[U, A]) =
    Schema.obj(
      parent.name + alt.label + "Case"
    )(fa =>
      List(
        Schema
          .field[A](alt.label)(a => a)(alt.instance.compile(this), fa)
      )
    )

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      tag: schema.EnumTag,
      values: List[schema.EnumValue[E]],
      total: E => schema.EnumValue[E]
  ): Schema[Any, E] = tag match {
    case StringEnum =>
      Schema.stringSchema.contramap(total(_).stringValue)
    case IntEnum =>
      Schema.intSchema.contramap(total(_).intValue)
  }
}
