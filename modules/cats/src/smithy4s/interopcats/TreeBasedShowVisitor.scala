package smithy4s.interopcats

import cats.Show
import cats.implicits.toContravariantOps
import smithy4s.{Bijection, Hints, Lazy, Refinement, ShapeId}
import smithy4s.schema.{AltF, CollectionTag, EnumTag, EnumValue, FieldF, MapTag, OptionalTag, Primitive, VisitorF}
import smithy4s.interopcats.instances.ShowInstances._

object TreeBasedShowVisitor extends VisitorF[Show] {
  def lazyShow[A](aShow: => Show[A]): Show[A] = new Show[A] {
    lazy val x: Show[A] = aShow
    override def show(t: A): String = x.show(t)
  }

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): Show[P] = lazyShow {
    primShowPf(tag)
  }
  override def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Lazy[Show[A]]
  ): Show[C[A]] = lazyShow {
    implicit val showA: Show[A] = member.value
    tag match {
      case CollectionTag.ListTag   => Show[List[A]]
      case CollectionTag.SetTag    => Show[Set[A]]
      case CollectionTag.VectorTag => Show[Vector[A]]
      case CollectionTag.IndexedSeqTag =>
        Show.show { seq =>
          seq.map(showA.show).mkString("IndexedSeq(", ", ", ")")
        }
    }
  }

  override def map[C[_, _], K, V](shapeId: ShapeId, hints: Hints, tag: MapTag[C], key: Lazy[Show[K]], value: Lazy[Show[V]]): Show[C[K, V]] =  lazyShow {
    implicit val showKey: Show[K] = key.value
    implicit val showValue: Show[V] = value.value
    Show[Map[K, V]].contramap(c => tag.toScalaMap(c))
  }

  override def enumeration[E](shapeId: ShapeId, hints: Hints, tag: EnumTag[E], values: List[EnumValue[E]]): Show[E] = Show.show { e =>
    tag match {
      case EnumTag.StringEnum(value, _) => value(e)
      case EnumTag.IntEnum(value, _) => value(e).toString
    }
  }
  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[FieldF[S, _, Show[Any], Any]],
      make: IndexedSeq[Any] => S
  ): Show[S] = lazyShow {
    def compileField[A](field: FieldF[S, A, Show[A], Any]): S => String = {
      val showField = field.schema.value.contramap(field.get)
      s => s"${field.label} = ${showField.show(s)}"
    }
    val functions = fields.map(f => compileField(f.asInstanceOf[FieldF[S,Any,Show[Any], Any]]))
    Show.show { s =>
      val values = functions
        .map(f => f(s))
        .map { case (value) => s"$value" }
        .mkString("(", ", ", ")")
      s"${shapeId.name}$values"
    }
  }
  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[AltF[U, _, Show[Any], Any]],
      dispatch: U => Int
  ): Show[U] = lazyShow {
    Show.show[U] { u =>
      val alt = alternatives(dispatch(u))
      alt.schema.value.show(alt.project(u))
    }
  }
  override def biject[A, B](
      schema: Lazy[Show[A]],
      bijection: Bijection[A, B]
  ): Show[B] = lazyShow {
    schema.value.contramap(bijection.from)
  }
  override def refine[A, B](
      schema: Lazy[Show[A]],
      refinement: Refinement[A, B]
  ): Show[B] = lazyShow {
    schema.value.contramap(refinement.from)
  }
  override def lazily[A](suspend: Lazy[Show[A]]): Show[A] = lazyShow(
    suspend.value
  )

  override def option[C[_], A](tag: OptionalTag[C], schema: Lazy[Show[A]]): Show[C[A]] = lazyShow {
    Show.show[Option[A]] {
      case None        => "None"
      case Some(value) => s"Some(${schema.value.show(value)})"
    }.contramap(c => tag.toScalaOption(c))
  }
}
