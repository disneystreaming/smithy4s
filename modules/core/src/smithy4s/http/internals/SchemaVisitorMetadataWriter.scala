package smithy4s
package http
package internals

import smithy4s.http.HttpBinding
import smithy4s.http.internals.MetaEncode._
import smithy4s.internals.InputOutput
import smithy4s.schema.Alt.SchemaAndValue
import smithy4s.schema.{EnumValue, Field, Primitive, SchemaAlt, SchemaField, SchemaVisitor}

import java.util.Base64

object SchemaVisitorMetadataWriter extends SchemaVisitor[MetaEncode] { self =>

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): MetaEncode[P] = {
    tag match {
      case Primitive.PBlob =>
        StringValueMetaEncode[ByteArray](ba =>
          Base64.getEncoder.encodeToString(ba.array)
        )
      case Primitive.PTimestamp =>
        (
          hints.get(HttpBinding).map(_.tpe),
          hints.get(smithy.api.TimestampFormat)
        ) match {
          case (_, Some(timestampFormat)) =>
            StringValueMetaEncode((timestamp: Timestamp) =>
              timestamp.format(timestampFormat)
            )
          case (Some(HttpBinding.Type.QueryType), None) |
              (Some(HttpBinding.Type.PathType), None) =>
            // See https://awslabs.github.io/smithy/1.0/spec/core/http-traits.html?highlight=httpquery#httpquery-trait
            StringValueMetaEncode((timestamp: Timestamp) =>
              timestamp.format(smithy.api.TimestampFormat.DATE_TIME)
            )
          case (Some(HttpBinding.Type.HeaderType), None) =>
            // See https://awslabs.github.io/smithy/1.0/spec/core/http-traits.html?highlight=httpquery#httpheader-trait
            StringValueMetaEncode((timestamp: Timestamp) =>
              timestamp.format(smithy.api.TimestampFormat.HTTP_DATE)
            )
          case _ => MetaEncode.empty
        }
      case Primitive.PDocument => MetaEncode.empty[P]
      case Primitive.PUnit     => MetaEncode.empty[P]
      case _                   => fromToString[P]
    }
  }

  override def list[A](
      shapeId: ShapeId,
      hints: Hints,
      member: Schema[A]
  ): MetaEncode[List[A]] = {
    self(member) match {
      case StringValueMetaEncode(f) =>
        StringListMetaEncode[List[A]](listA => listA.map(f))
      case _ => MetaEncode.empty
    }
  }

  override def set[A](
      shapeId: ShapeId,
      hints: Hints,
      member: Schema[A]
  ): MetaEncode[Set[A]] = {
    self(member) match {
      case StringValueMetaEncode(f) =>
        StringListMetaEncode[Set[A]](set => set.map(f).toList)
      case _ => MetaEncode.empty
    }
  }

  override def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): MetaEncode[Map[K, V]] = {
    (self(key), self(value)) match {
      case (StringValueMetaEncode(keyF), StringValueMetaEncode(valueF)) =>
        StringMapMetaEncode(map =>
          map.map { case (k, v) =>
            (keyF(k), valueF(v))
          }
        )
      case (StringValueMetaEncode(keyF), StringListMetaEncode(valueF)) =>
        StringListMapMetaEncode(map =>
          map.map { case (k, v) =>
            (keyF(k), valueF(v))
          }
        )
      case _ => MetaEncode.empty
    }
  }

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): MetaEncode[E] = {
    StringValueMetaEncode(e => total(e).stringValue)
  }

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): MetaEncode[S] = {
    val maybeInputOutput = hints.get[InputOutput]
    def encodeField[A](
        field: SchemaField[S, A]
    ): Option[(Metadata, S) => Metadata] = {
      val hints = field.instance.hints
      HttpBinding
        .fromHints(field.label, hints, maybeInputOutput)
        .map { binding =>
          val folderT = new Field.LeftFolder[Schema, Metadata] {
            override def compile[T](
                label: String,
                instance: Schema[T]
            ): (Metadata, T) => Metadata = {
              val encoder: MetaEncode[T] = self(instance)
              val updateFunction = encoder.updateMetadata(binding)
              (metadata, t: T) => updateFunction(metadata, t)
            }
          }
          field.leftFolder(folderT)
        }
    }

    val updateFunctions =
      fields.map(field => encodeField(field)).collect {
        case Some(updateFunction) =>
          updateFunction
      }

    StructureMetaEncode(s =>
      updateFunctions.foldLeft(Metadata.empty)((metadata, updateFunction) =>
        updateFunction(metadata, s)
      )
    )
  }

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[U, _]],
      dispatch: U => SchemaAndValue[U, _]
  ): MetaEncode[U] = MetaEncode.empty

  override def biject[A, B](
      schema: Schema[A],
      to: A => B,
      from: B => A
  ): MetaEncode[B] = self(schema).contramap(from)

  override def surject[A, B](
      schema: Schema[A],
      to: Refinement[A, B],
      from: B => A
  ): MetaEncode[B] = self(schema).contramap(from)

  override def lazily[A](suspend: Lazy[Schema[A]]): MetaEncode[A] =
    MetaEncode.empty
}
