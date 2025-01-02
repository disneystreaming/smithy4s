/*
 *  Copyright 2021-2025 Disney Streaming
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

package smithy4s.dynamic.internals.conversion

import smithy4s.schema.SchemaVisitor
import software.amazon.smithy.model.shapes.Shape
import smithy4s.{ShapeId => ScalaShapeId, _}
import smithy4s.schema.{Schema => _, _}
import smithy4s.schema.Primitive._
import software.amazon.smithy.model.shapes._
import java.util.function.Consumer

/**
  * When encountering a Schema, a recorder is meant to add the shape
  * corresponding to the schema in a record, and returns the id of the shape.
  * (which will be used to record other shapes).
  */
private[dynamic] trait ShapeRecorder[A]
    extends (Map[ShapeId, Shape] => (Map[ShapeId, Shape], A)) { self =>

  def record(f: A => Shape): ShapeRecorder[ShapeId] =
    new ShapeRecorder[ShapeId] {
      def apply(record: Map[ShapeId, Shape]): (Map[ShapeId, Shape], ShapeId) = {
        val (rec, a) = self(record)
        val newShape = f(a)
        val id = newShape.getId()
        (rec + (id -> newShape)) -> id
      }
    }

  def map[B](f: A => B) = new ShapeRecorder[B] {
    def apply(record: Map[ShapeId, Shape]): (Map[ShapeId, Shape], B) = {
      val (aRecord, a) = self(record)
      val b = f(a)
      (aRecord, b)
    }
  }

  def flatMap[B](f: A => ShapeRecorder[B]) = new ShapeRecorder[B] {
    def apply(record: Map[ShapeId, Shape]): (Map[ShapeId, Shape], B) = {
      val (aRecord, a) = self(record)
      val bRecorder = f(a)
      val (bRecord, b) = bRecorder(aRecord)
      (bRecord, b)
    }
  }

  def zip[B](other: ShapeRecorder[B]): ShapeRecorder[(A, B)] =
    this.flatMap { a => other.map(a -> _) }

}

object ShapeRecorder {

  def sequence[A](
      recorders: Seq[ShapeRecorder[A]]
  ): ShapeRecorder[Seq[A]] = new ShapeRecorder[Seq[A]] {
    def apply(map: Map[ShapeId, Shape]): (Map[ShapeId, Shape], Seq[A]) = {
      var record = map
      val buffer = Vector.newBuilder[A]
      recorders.foreach { recorder =>
        val (newRecord, a) = recorder(record)
        record = newRecord
        buffer += a
      }
      (record, buffer.result())
    }
  }

}

private[dynamic] object ToSmithyVisitor extends SchemaVisitor[ToSmithy] {
  self =>

  import syntax._

  private def record(shp: Shape): ShapeRecorder[ShapeId] =
    new ShapeRecorder[ShapeId] {
      def apply(
          recorded: Map[ShapeId, Shape]
      ): (Map[ShapeId, Shape], ShapeId) = {
        val id = shp.getId()
        val newVisited = recorded + (id -> shp)
        newVisited -> id
      }
    }

  def primitive[P](
      shapeId: ScalaShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): ShapeRecorder[ShapeId] = {
    def shp = tag match {
      case PUUID       => StringShape.builder().setId(shapeId).build()
      case PBigDecimal => BigDecimalShape.builder().setId(shapeId).build()
      case PBigInt     => BigIntegerShape.builder().setId(shapeId).build()
      case PString     => StringShape.builder().setId(shapeId).build()
      case PDouble     => DoubleShape.builder().setId(shapeId).build()
      case PInt        => IntegerShape.builder().setId(shapeId).build()
      case PBlob       => BlobShape.builder().setId(shapeId).build()
      case PTimestamp  => TimestampShape.builder().setId(shapeId).build()
      case PFloat      => FloatShape.builder().setId(shapeId).build()
      case PLong       => BigDecimalShape.builder().setId(shapeId).build()
      case PByte       => ByteShape.builder().setId(shapeId).build()
      case PDocument   => DocumentShape.builder().setId(shapeId).build()
      case PBoolean    => BooleanShape.builder().setId(shapeId).build()
      case PShort      => ShortShape.builder().setId(shapeId).build()
    }
    record(shp.captureHints(hints.targetHints))
  }

  private def addHintsConsumer(hints: Hints): Consumer[MemberShape.Builder] =
    new Consumer[MemberShape.Builder]() {
      override def accept(builder: MemberShape.Builder) = {
        val _ = builder.addTraits(hints.asTraits)
      }
    }

  def collection[C[_], A](
      shapeId: ScalaShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): ShapeRecorder[ShapeId] =
    member.compile(this).record { visited =>
      ListShape
        .builder()
        .setId(shapeId)
        .member(
          member.shapeId.toSmithy,
          addHintsConsumer(member.hints.memberHints)
        )
        .build()
        .captureHints(hints.targetHints)
    }

  def map[K, V](
      shapeId: ScalaShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): ShapeRecorder[ShapeId] =
    self(key).zip(self(value)).record { case (keyId, valueId) =>
      MapShape
        .builder()
        .setId(shapeId)
        .key(keyId, addHintsConsumer(key.hints.memberHints))
        .value(valueId, addHintsConsumer(value.hints.memberHints))
        .build()
        .captureHints(hints.targetHints)
    }

  def enumeration[E](
      shapeId: ScalaShapeId,
      hints: Hints,
      tag: EnumTag[E],
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): ShapeRecorder[ShapeId] = {
    val shape = tag match {
      case EnumTag.IntEnum() =>
        val builder = IntEnumShape.builder().id(shapeId.toSmithy)
        values.foreach { value =>
          builder.addMember(
            value.name,
            value.intValue,
            addHintsConsumer(value.hints)
          )
        }
        builder.build()
      case _ =>
        val builder = EnumShape.builder().id(shapeId.toSmithy)
        values.foreach { value =>
          builder.addMember(
            value.name,
            value.stringValue,
            addHintsConsumer(value.hints)
          )
        }
        builder.build()
    }
    record(shape.captureHints(hints.targetHints))
  }

  def struct[S](
      shapeId: ScalaShapeId,
      hints: Hints,
      fields: Vector[Field[S, _]],
      make: IndexedSeq[Any] => S
  ): ShapeRecorder[ShapeId] =
    ShapeRecorder.sequence(fields.map(f => self(f.schema))).record {
      targetIds =>
        val builder = StructureShape.builder().setId(shapeId)
        fields.zip(targetIds).foreach { case (field, targetId) =>
          val member = MemberShape
            .builder()
            .id(
              ShapeId.fromParts(shapeId.namespace, shapeId.name, field.label)
            )
            .target(targetId)
            .build()
          builder.addMember(addTraits(member, field.memberHints))
        }
        builder.build().captureHints(hints.targetHints)
    }

  def union[U](
      shapeId: ScalaShapeId,
      hints: Hints,
      alternatives: Vector[Alt[U, _]],
      dispatch: Alt.Dispatcher[U]
  ): ShapeRecorder[ShapeId] =
    ShapeRecorder.sequence(alternatives.map(a => self(a.schema))).record {
      targetId =>
        val builder: UnionShape.Builder = UnionShape.builder().setId(shapeId)
        alternatives.zip(targetId).foreach { case (alt, targetId) =>
          val member = MemberShape
            .builder()
            .id(
              ShapeId.fromParts(shapeId.namespace, shapeId.name, alt.label)
            )
            .target(targetId)
            .build()
          builder.addMember(addTraits(member, alt.memberHints))
        }
        builder.build().captureHints(hints.targetHints)
    }

  def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): ShapeRecorder[ShapeId] =
    self(schema)

  def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): ShapeRecorder[ShapeId] = self(schema)

  def lazily[A](suspend: Lazy[Schema[A]]): ShapeRecorder[ShapeId] = {
    val rec = suspend.map(s => s.shapeId -> this.apply(s))
    val placeholderId = ShapeId.fromParts("placeholder", "Placeholder")
    val placeholderShape = StructureShape.builder().id(placeholderId).build()
    new ShapeRecorder[ShapeId] {
      def apply(
          record: Map[ShapeId, Shape]
      ): (Map[ShapeId, Shape], ShapeId) = {
        val (schema, f) = rec.value
        val shapeId = schema.id.toSmithy
        record.get(shapeId) match {
          case None =>
            f(record + (shapeId -> placeholderShape))
          case Some(_) =>
            // Here we're stopping the recursion, we've already traversed the recursive schema once,
            // we just trust that the other layers do their thing.
            record -> shapeId
        }
      }
    }
  }

  def option[A](schema: Schema[A]): ShapeRecorder[ShapeId] =
    self(schema)

}
