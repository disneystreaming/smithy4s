package smithy4s.interopcats

import cats.Hash
import cats.implicits.{
  catsKernelStdHashForList,
  catsKernelStdHashForOption,
  toContravariantOps
}
import smithy4s.{Bijection, Hints, Lazy, Refinement, ShapeId}
import smithy4s.capability.EncoderK
import smithy4s.interopcats.instances.HashInstances._
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
object SchemaVisitorHash extends SchemaVisitor[Hash] { self =>

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): Hash[P] = primHashPf(tag)

  override def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): Hash[C[A]] = {
    implicit val memberHash: Hash[A] = self(member)
    tag match {
      case CollectionTag.ListTag       => Hash[List[A]]
      case CollectionTag.SetTag        => Hash[Set[A]]
      case CollectionTag.VectorTag     => Hash[Vector[A]]
      case CollectionTag.IndexedSeqTag => Hash[Seq[A]].contramap(_.toIndexedSeq)
    }
  }

  override def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): Hash[Map[K, V]] = {
    implicit val keyHash: Hash[K] = self(key)
    implicit val valueHash: Hash[V] = self(value)
    Hash[Map[K, V]]
  }

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): Hash[E] = {
    implicit val enumValueHash: Hash[EnumValue[E]] =
      Hash[String].contramap(_.stringValue)
    Hash[EnumValue[E]].contramap(total)
  }

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): Hash[S] = {
    new Hash[S] {
      override def hash(x: S): Int = {
        def forField[A2](field: Field[Schema, S, A2]): Hash[S] = {
          val hashField: Hash[A2] =
            field.foldK(new Field.FolderK[Schema, S, Hash]() {
              override def onRequired[A](
                  label: String,
                  instance: Schema[A],
                  get: S => A
              ): Hash[A] = self(instance)

              override def onOptional[A](
                  label: String,
                  instance: Schema[A],
                  get: S => Option[A]
              ): Hash[Option[A]] = {
                implicit val hashA: Hash[A] = self(instance)
                Hash[Option[A]]
              }
            })
          hashField.contramap(field.get)
        }
        fields.map(field => forField(field)).map(hash => hash.hash(x)).sum
      }
      override def eqv(x: S, y: S): Boolean =
        self.struct(shapeId, hints, fields, make).eqv(x, y)

    }
  }

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[U, _]],
      dispatch: Alt.Dispatcher[Schema, U]
  ): Hash[U] = {

    // A version of `Hash` that assumes for the eqv method that the RHS is "up-casted" to U.
    trait AltHash[A] {
      def eqv(a: A, u: U): Boolean
      def hash(a:A):Int
    }

    // The encoded form that Eq works against is a partially-applied curried function.
    implicit val encoderKInstance = new EncoderK[AltHash, (U => Boolean,Int)] {
      def apply[A](fa: AltHash[A], a: A): (U => Boolean,Int) = {
        ((u: U) =>
        fa.eqv(a, u),fa.hash(a))
      }

      def absorb[A](f: A => (U => Boolean,Int)): AltHash[A] = new AltHash[A] {
        def eqv(a: A, u: U): Boolean = f(a)._1(u)
        override def hash(a:A): Int = f(a)._2
      }
    }

    val precompiler = new Alt.Precompiler[Schema, AltHash] {
      def apply[A](label: String, instance: Schema[A]): AltHash[A] = {
        // Here we "cheat" to recover the `Alt` corresponding to `A`, as this information
        // is lost in the precompiler.
        val altA =
        alternatives.find(_.label == label).get.asInstanceOf[SchemaAlt[U, A]]
        // We're using it to get a function that lets us project the `U` against `A`.
        // `U` is not necessarily an `A, so this function returns an `Option`
        val projectA: U => Option[A] = dispatch.projector(altA)
        val hashA = instance.compile(self)
        new AltHash[A] {
          def eqv(a: A, u: U): Boolean = projectA(u) match {
            case None => false // U is not an A.
            case Some(a2) =>
              hashA.eqv(a, a2) // U is an A, we delegate the comparison
          }
          override def hash(a:A): Int = hashA.hash(a)
        }
      }
    }

    val altHashU: AltHash[U] = dispatch.compile(precompiler)
    new Hash[U] {
      def eqv(x: U, y: U): Boolean = altHashU.eqv(x, y)
      def hash(x: U): Int = altHashU.hash(x)
    }
  }


  override def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): Hash[B] = {
    implicit val hashA: Hash[A] = self(schema)
    Hash[A].contramap(bijection.from)
  }

  override def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): Hash[B] = {
    implicit val hashA: Hash[A] = self(schema)
    Hash[A].contramap(refinement.from)
  }

  override def lazily[A](suspend: Lazy[Schema[A]]): Hash[A] = {
    implicit val hashA: Lazy[Hash[A]] = suspend.map(self(_))
    new Hash[A] {
      override def hash(x: A): Int = hashA.value.hash(x)
      override def eqv(x: A, y: A): Boolean = hashA.value.eqv(x, y)
    }
  }
}