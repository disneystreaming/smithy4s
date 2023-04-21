package smithy4s.interopcats

import cats.Show
import cats.implicits.toContravariantOps
import smithy4s._
import smithy4s.capability.EncoderK
import smithy4s.interopcats.instances.ShowInstances._
import smithy4s.schema.{
  Alt,
  CollectionTag,
  EnumValue,
  Field,
  Primitive,
  Schema,
  SchemaAlt,
  SchemaField,
  SchemaVisitor
}
import smithy4s.schema.Alt.Precompiler

object SchemaVisitorShow extends SchemaVisitor[Show] { self =>

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): Show[P] = primShowPf(tag)

  override def collection[C[`2`], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): Show[C[A]] = {
    implicit val showSchemaA: Show[A] = self(member)
    tag match {
      case CollectionTag.ListTag => Show[List[A]]

      case CollectionTag.SetTag => Show[Set[A]]

      case CollectionTag.VectorTag => Show[Vector[A]]

      case CollectionTag.IndexedSeqTag => Show[Seq[A]].contramap(_.toIndexedSeq)
    }
  }

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[U, _]],
      dispatch: Alt.Dispatcher[Schema, U]
  ): Show[U] = {

    val precomputed: Precompiler[Schema, Show] = new Precompiler[Schema, Show] {
      override def apply[A](label: String, instance: Schema[A]): Show[A] = {
        self(instance)
      }
    }
    implicit val encoderKShow: EncoderK[Show, String] =
      new EncoderK[Show, String] {
        override def apply[A](fa: Show[A], a: A): String = fa.show(a)

        override def absorb[A](f: A => String): Show[A] = Show.show(f)
      }

    dispatch.compile(precomputed)

  }

  override def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): Show[B] = {
    self(schema).contramap(bijection.from)
  }

  override def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): Show[B] =
    self(schema).contramap(refinement.from)

  override def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): Show[Map[K, V]] = {
    implicit val showKey: Show[K] = self(key)
    implicit val showValue: Show[V] = self(value)
    Show[Map[K, V]]
  }

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): Show[E] = Show.show { e =>
    total(e).stringValue
  }

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): Show[S] = {
    def compileField[A](
        schemaField: SchemaField[S, A]
    ): S => String = {
      val folder = new Field.FolderK[Schema, S, Show]() {
        override def onRequired[AA](
            label: String,
            instance: Schema[AA],
            get: S => AA
        ): Show[AA] = self(instance)

        override def onOptional[AA](
            label: String,
            instance: Schema[AA],
            get: S => Option[AA]
        ): Show[Option[AA]] = {
          implicit val showAA: Show[AA] = self(instance)
          Show[Option[AA]]

        }
      }
      val showField = schemaField.foldK(folder)
      s => showField.show(schemaField.get(s))
    }

    val functions = fields.map { f => compileField(f) }
    Show.show { s =>
      val values = functions
        .map(f => f(s))
        .map { case (value) => s"$value" }
        .mkString("(", ",", ")")
      s"${shapeId.name}$values"
    }
  }

  override def lazily[A](suspend: Lazy[Schema[A]]): Show[A] = Show.show[A] {
    val ss = suspend.map {
      self(_)
    }
    a => ss.value.show(a)
  }

}
