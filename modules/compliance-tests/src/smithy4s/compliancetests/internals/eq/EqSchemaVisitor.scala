package smithy4s.compliancetests.internals.eq

import smithy4s.compliancetests.internals.eq.Smithy4sEqInstances._
import cats.implicits.catsSyntaxEq
import cats.kernel.Eq
import smithy4s._
import smithy4s.schema.{Schema, _}

import java.util.UUID

object EqSchemaVisitor extends SchemaVisitor[Eq] { self =>
  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): Eq[P] = primitiveEq(tag)

  override def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): Eq[C[A]] = {
    implicit val memberEq: Eq[A] = self(member)
    tag match {
      case CollectionTag.ListTag       => Eq[List[A]]
      case CollectionTag.SetTag        => Eq[Set[A]]
      case CollectionTag.VectorTag     => Eq[Vector[A]]
      case CollectionTag.IndexedSeqTag => Eq[IndexedSeq[A]]
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
      case Primitive.PShort      => Eq[Short]
      case Primitive.PInt        => Eq[Int]
      case Primitive.PFloat      => Eq[Float]
      case Primitive.PLong       => Eq[Long]
      case Primitive.PDouble     => Eq[Double]
      case Primitive.PBigInt     => Eq[BigInt]
      case Primitive.PBigDecimal => Eq[BigDecimal]
      case Primitive.PBoolean    => Eq[Boolean]
      case Primitive.PString     => Eq[String]
      case Primitive.PUUID       => Eq[UUID]
      case Primitive.PByte       => Eq[Byte]
      case Primitive.PBlob       => Eq[ByteArray]
      case Primitive.PDocument   => Eq[Document]
      case Primitive.PTimestamp  => Eq[Timestamp]
      case Primitive.PUnit       => Eq[Unit]
    }
  }

}
