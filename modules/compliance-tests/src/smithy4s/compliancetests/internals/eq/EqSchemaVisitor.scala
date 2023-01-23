package smithy4s.compliancetests.internals.eq

import smithy4s.compliancetests.internals.eq.Smithy4sEqInstances._
import cats.implicits.catsSyntaxEq
import cats.kernel.Eq
import smithy4s._
import smithy4s.schema.{Schema, _}

object EqSchemaVisitor extends SchemaVisitor[Eq] { self =>
  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): Eq[P] = primitiveEq(tag)

  override def collection[C[`2`], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): Eq[C[A]] = {
    implicit val memberEq: Eq[A] = self(member)
    tag match {
      case CollectionTag.ListTag       => Eq[C[A]]
      case CollectionTag.SetTag        => Eq[C[A]]
      case CollectionTag.VectorTag     => Eq[C[A]]
      case CollectionTag.IndexedSeqTag => Eq[C[A]]
    }
  }

  override def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): Eq[Map[K, V]] = {
    implicit val valueEq: Eq[V] = self(value)
    Eq[Map[K, V]]
  }

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): Eq[E] =
    (x: E, y: E) => {
      val enumX = total(x)
      val enumY = total(y)
      enumX.intValue === enumY.intValue && enumX.stringValue === enumY.stringValue
    }

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): Eq[S] = { (x: S, y: S) =>
    {
      def forField[A2](field: Field[Schema, S, A2]): Boolean = {
        val eqField = field.foldK(new Field.FolderK[Schema, S, Eq]() {
          override def onRequired[A](
              label: String,
              instance: Schema[A],
              get: S => A
          ): Eq[A] = self(instance)

          override def onOptional[A](
              label: String,
              instance: Schema[A],
              get: S => Option[A]
          ): Eq[Option[A]] = {
            val showA = self(instance)
            (x: Option[A], y: Option[A]) =>
              (x, y) match {
                case (Some(a), Some(b)) => showA.eqv(a, b)
                case (None, None)       => true
                case _                  => false
              }
          }
        })
        eqField.eqv(field.get(x), field.get(y))
      }

      fields.forall(forField(_))

    }

  }

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[U, _]],
      dispatch: Alt.Dispatcher[Schema, U]
  ): Eq[U] = {
    // just to make sure we don't forget to implement this
    throw new NotImplementedError("EqSchemaVisitor does not support unions")
  }

  override def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): Eq[B] = {
    val aEq: Eq[A] = self(schema)
    (x: B, y: B) => aEq.eqv(bijection.from(x), bijection.from(y))
  }

  override def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): Eq[B] = {
    val eqA: Eq[A] = self(schema)
    (x: B, y: B) => eqA.eqv(refinement.from(x), refinement.from(y))
  }

  override def lazily[A](suspend: Lazy[Schema[A]]): Eq[A] = {
    val eq = suspend.map(self(_))
    (x: A, y: A) => eq.value.eqv(x, y)
  }

  def primitiveEq[P](primitive: Primitive[P]): Eq[P] = {
    primitive match {
      case Primitive.PShort      => Eq[P]
      case Primitive.PInt        => Eq[P]
      case Primitive.PFloat      => Eq[P]
      case Primitive.PLong       => Eq[P]
      case Primitive.PDouble     => Eq[P]
      case Primitive.PBigInt     => Eq[P]
      case Primitive.PBigDecimal => Eq[P]
      case Primitive.PBoolean    => Eq[P]
      case Primitive.PString     => Eq[P]
      case Primitive.PUUID       => Eq[P]
      case Primitive.PByte       => Eq[P]
      case Primitive.PBlob       => Eq[P]
      case Primitive.PDocument   => Eq[P]
      case Primitive.PTimestamp  => Eq[P]
      case Primitive.PUnit       => Eq[P]
    }
  }

}
