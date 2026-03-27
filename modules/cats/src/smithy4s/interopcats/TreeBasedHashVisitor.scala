package smithy4s.interopcats

import smithy4s.schema._
import cats.{Eq, Hash}
import cats.implicits._
import smithy4s.interopcats.instances.HashInstances.primHashPf
import smithy4s.{Bijection, Hints, Lazy, Refinement, ShapeId}

import scala.util.hashing.MurmurHash3.productSeed

object TreeBasedHashVisitor extends VisitorF[Hash] {

  def lazyHash[A](underlying: => Hash[A]): Hash[A] = new Hash[A] {
    lazy val z = underlying
    override def hash(x: A): Int = z.hash(x)
    override def eqv(x: A, y: A): Boolean = z.eqv(x, y)
  }
  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): Hash[P] = primHashPf(tag)
  override def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Lazy[Hash[A]]
  ): Hash[C[A]] = {
    lazyHash {
      implicit val memberHash: Hash[A] = member.value
      tag match {
        case CollectionTag.ListTag   => Hash[List[A]]
        case CollectionTag.SetTag    => Hash[Set[A]]
        case CollectionTag.VectorTag => Hash[Vector[A]]
        case CollectionTag.IndexedSeqTag =>
          Hash[scala.collection.immutable.Seq[A]].contramap(_.toIndexedSeq)
      }
    }
  }

  override def map[C[_, _], K, V](shapeId: ShapeId, hints: Hints, tag: MapTag[C], key: Lazy[Hash[K]], value: Lazy[Hash[V]]): Hash[C[K, V]] =  {
    lazyHash {
      Eq.catsKernelHashForMap(key.value, value.value).contramap(c => tag.toScalaMap(c))
    }
  }

  override def enumeration[E](shapeId: ShapeId, hints: Hints, tag: EnumTag[E], values: List[EnumValue[E]]): Hash[E] = {
    tag match {
      case EnumTag.IntEnum(value, _) =>
        Hash[Int].contramap(value)
      case EnumTag.StringEnum(value, _) =>
        Hash[String].contramap(value)
    }
  }
  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[FieldF[S, _, Hash[Any], Any]],
      make: IndexedSeq[Any] => S
  ): Hash[S] = lazyHash {
    def forField[A2](field: FieldF[S, A2, Hash[A2], Any]): Hash[S] = {
      field.schema.value.contramap(field.get)
    }
    val hashInstances: Vector[Hash[S]] = fields.map(field => forField(field.asInstanceOf[FieldF[S,Any,Hash[Any], Any]]))
    val nameHash = Hash[String].hash(shapeId.name)
    new Hash[S] {
      override def hash(x: S): Int = {
        val hashCodes = hashInstances.map(_.hash(x))
        combineHash(productSeed, nameHash +: hashCodes: _*)
      }
      override def eqv(x: S, y: S): Boolean =
        hashInstances.forall(_.eqv(x, y))
    }
  }

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[AltF[U, _, Hash[Any], Any]],
      dispatch: U => Int
  ): Hash[U] = {
    lazyHash {
      new Hash[U] {
        override def hash(x: U): Int = {
          val idx = dispatch(x)
          alternatives(idx).schema.value.hash(x)
        }
        override def eqv(x: U, y: U): Boolean = {
          val idx = dispatch(x)
          val alt = alternatives(idx)
          alt.project.lift(y) match {
            case Some(_) => alt.schema.value.eqv(x, y)
            case None    => false
          }
        }
      }
    }
  }

  override def biject[A, B](
      schema: Lazy[Hash[A]],
      bijection: Bijection[A, B]
  ): Hash[B] = {
    lazyHash {
      new Hash[B] {
        val bijected = schema.value.contramap(bijection.from)
        override def hash(x: B): Int = bijected.hash(x)
        override def eqv(x: B, y: B): Boolean = bijected.eqv(x, y)
      }
    }

  }

  override def refine[A, B](
      schema: Lazy[Hash[A]],
      refinement: Refinement[A, B]
  ): Hash[B] = lazyHash {
    schema.value.contramap(refinement.from)
  }

  override def lazily[A](suspend: Lazy[Hash[A]]): Hash[A] = lazyHash {
    suspend.value
  }

  override def option[C[_], A](tag: OptionalTag[C], schema: Lazy[Hash[A]]): Hash[C[A]] = lazyHash {
    new Hash[C[A]] {
      val optioned =
        cats.instances.option.catsKernelStdHashForOption(schema.value)
      override def hash(x: C[A]): Int = optioned.hash(tag.toScalaOption(x))
      override def eqv(x: C[A], y: C[A]): Boolean = optioned.eqv(tag.toScalaOption(x), tag.toScalaOption(y))
    }
  }
}
