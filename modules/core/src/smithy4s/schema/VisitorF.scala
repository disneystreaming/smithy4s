package smithy4s.schema

import smithy4s.capability.{Cache, MonadThrowLike}
import smithy4s._
import smithy4s.schema.VisitorF.Aux

/**
 * An effectful visitor for schemas.
 */
trait VisitorF[O[_]] extends VisitorRF[Any, O] {
  def primitive[P](shapeId: ShapeId, hints: Hints, tag: Primitive[P]): O[P]
  def collection[C[_], A](shapeId: ShapeId, hints: Hints, tag: CollectionTag[C], member: Lazy[O[A]]): O[C[A]]
  def map[C[_, _], K, V](shapeId: ShapeId, hints: Hints, tag: MapTag[C], key: Lazy[O[K]], value: Lazy[O[V]]): O[C[K, V]]
  def enumeration[E](shapeId: ShapeId, hints: Hints, tag: EnumTag[E], values: List[EnumValue[E]]): O[E]
  def struct[S](shapeId: ShapeId, hints: Hints, fields: Vector[FieldF[S, _, O[Any], Any]], make: IndexedSeq[Any] => S): O[S]
  def union[U](shapeId: ShapeId, hints: Hints, alternatives: Vector[AltF[U, _, O[Any], Any]], dispatch: U => Int): O[U]
  def biject[A, B](underlying: Lazy[O[A]], bijection: Bijection[A, B]): O[B]
  def refine[A, B](underlying: Lazy[O[A]], refinement: Refinement[A, B]): O[B]
  def lazily[A](suspend: Lazy[O[A]]): O[A]
  def option[C[_], A](tag: OptionalTag[C], member: Lazy[O[A]]): O[C[A]]

  final override def primitive[P](shapeId: ShapeId, hints: Hints, tag: Primitive[P], in: Any): O[P] =
    primitive(shapeId,hints,tag)

  final override def collection[C[_], A](shapeId: ShapeId, hints: Hints, tag: CollectionTag[C], member: Lazy[O[A]], memberIn: Any, in: Any): O[C[A]] =
    collection(shapeId,hints,tag,member)

  final override def map[C[_, _], K, V](shapeId: ShapeId, hints: Hints, tag: MapTag[C], key: Lazy[O[K]], value: Lazy[O[V]], keyIn: Any, valueIn: Any, in: Any): O[C[K, V]] =
    map(shapeId,hints,tag,key,value)

  final override def enumeration[E](shapeId: ShapeId, hints: Hints, tag: EnumTag[E], values: List[EnumValue[E]], in: Any): O[E] =
    enumeration(shapeId,hints,tag, values)

  final override def struct[S](shapeId: ShapeId, hints: Hints, fields: Vector[FieldF[S, _, O[Any], Any]], make: IndexedSeq[Any] => S, in: Any): O[S] =
    struct(shapeId,hints,fields,make)

  final override def union[U](shapeId: ShapeId, hints: Hints, alternatives: Vector[AltF[U, _, O[Any], Any]], dispatch: U => Int, in: Any): O[U] =
    union(shapeId,hints,alternatives,dispatch)

  final override def biject[A, B](underlying: Lazy[O[A]], bijection: Bijection[A, B], underlyingIn: Any, in: Any): O[B] =
    biject(underlying, bijection)

  final override def refine[A, B](underlying: Lazy[O[A]], refinement: Refinement[A, B], underlyingIn: Any, in: Any): O[B] =
    refine(underlying,refinement)

  final override def lazily[A](suspend: Lazy[O[A]], in: Lazy[Any]): O[A] =
    lazily(suspend)

  final override def option[C[_], A](tag: OptionalTag[C], member: Lazy[O[A]], memberIn: Any, in: Any): O[C[A]] =
    option(tag,member)

  final def apply[F[_]](
                         cache: Cache[F, O[_]]
                       )(implicit F: MonadThrowLike[F]): Schema ~> VisitorF.Aux[F, O, *] = {
    apply[F](cache, new (Schema ~> Any) {
      override def apply[A0](fa: Schema[A0]): Any = fa
    })
  }
}
object VisitorF {
  type Aux[F[_], G[_], A] = F[G[A]]
}


/**
 * An effectful visitor that can ask for an environment `I[_]` for any given schema.
 */
trait VisitorRF[I[_], O[_]] {
  def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P],
      in: I[P]
  ): O[P]
  def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Lazy[O[A]],
      memberIn: I[A],
      in: I[C[A]]
  ): O[C[A]]
  def map[C[_, _], K, V](
      shapeId: ShapeId,
      hints: Hints,
      tag: MapTag[C],
      key: Lazy[O[K]],
      value: Lazy[O[V]],
      keyIn: I[K],
      valueIn: I[V],
      in: I[C[K, V]]
  ): O[C[K, V]]
  def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      tag: EnumTag[E],
      values: List[EnumValue[E]],
      in: I[E]
  ): O[E]
  def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[FieldF[S, _, O[Any], I[Any]]],
      make: IndexedSeq[Any] => S,
      in: I[S]
  ): O[S]
  def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[AltF[U, _, O[Any], I[Any]]],
      dispatch: U => Int,
      in: I[U]
  ): O[U]
  def biject[A, B](
      underlying: Lazy[O[A]],
      bijection: Bijection[A, B],
      underlyingIn: I[A],
      in: I[B]
  ): O[B]
  def refine[A, B](
      underlying: Lazy[O[A]],
      refinement: Refinement[A, B],
      underlyingIn: I[A],
      in: I[B]
  ): O[B]
  def lazily[A](suspend: Lazy[O[A]], in: Lazy[I[A]]): O[A]
  def option[C[_], A](
      tag: OptionalTag[C],
      member: Lazy[O[A]],
      memberIn: I[A],
      in: I[C[A]]
  ): O[C[A]]

  final def apply[F[_]](
                         cache: Cache[F, O[_]],
                         ask: Schema ~> I
  )(implicit F: MonadThrowLike[F]): Schema ~> VisitorF.Aux[F, O, *] = {
    new (Schema ~> VisitorF.Aux[F, O, *]) {
      override def apply[A](schema: Schema[A]): Aux[F, O, A] = schema match {
        case s @ Schema.PrimitiveSchema(shapeId, hints, tag) =>
          F.pure(primitive(shapeId, hints, tag, ask(s)))
        case s @ Schema.CollectionSchema(shapeId, hints, tag, member) =>
          F.map(cache.get(member))(memberG =>
            collection(shapeId, hints, tag, memberG.asInstanceOf[Lazy[O[Any]]], ask(member), ask(s))
          )
        case s @ Schema.MapSchema(shapeId, hints, tag, key, value) =>
          F.flatMap(cache.get(key))(keyG =>
            F.map(cache.get(value))(valueG =>
              map(
                shapeId,
                hints,
                tag,
                keyG.asInstanceOf[Lazy[O[Any]]],
                valueG.asInstanceOf[Lazy[O[Any]]],
                ask(key),
                ask(value),
                ask(s)
              )
            )
          )
        case s @ Schema.EnumerationSchema(shapeId, hints, tag, values) =>
          F.pure(enumeration(shapeId, hints, tag, values, ask(s)))
        case s @ Schema.StructSchema(shapeId, hints, fields, make) =>
          val fieldFGs = fields.map { field =>
            F.map(cache.get(field.schema))(fieldG =>
              FieldF[A, Any, O[Any], I[Any]](
                field.label,
                fieldG.asInstanceOf[Lazy[O[Any]]],
                field.get,
                ask(field.schema.asInstanceOf[Schema[Any]])
              )
            )
          }
          F.map(vectorSequence(fieldFGs))(fieldGs =>
            struct(shapeId, hints, fieldGs, make, ask(s))
          )
        case s @ Schema.UnionSchema(shapeId, hints, alternatives, ordinal) =>
          val altFGs = alternatives.map(alt =>
            F.map(cache.get(alt.schema))(altF =>
              AltF[A, Any, O[Any], I[Any]](
                alt.label,
                altF.asInstanceOf[Lazy[O[Any]]],
                alt.inject.asInstanceOf[Any => A],
                alt.project,
                ask(alt.schema.asInstanceOf[Schema[Any]])
              )
            )
          )
          F.map(vectorSequence(altFGs))(altGs =>
            union(shapeId, hints, altGs, ordinal, ask(s))
          )
        case s @ Schema.OptionSchema(tag, underlying) =>
          F.map(cache.get(underlying))(underG =>
            option(tag, underG.asInstanceOf[Lazy[O[Any]]],ask(underlying), ask(s))
          )
        case s @ Schema.BijectionSchema(underlying, bijection) =>
          F.map(cache.get(underlying))(underG =>
            biject(underG.asInstanceOf[Lazy[O[Any]]], bijection, ask(underlying), ask(s))
          )
        case s @ Schema.RefinementSchema(underlying, refinement) =>
          F.map(cache.get(underlying))(underG =>
            refine(underG.asInstanceOf[Lazy[O[Any]]], refinement, ask(underlying), ask(s))
          )
        case Schema.LazySchema(suspend) =>
          F.map(cache.get(suspend.value))(susG =>
            lazily(susG.asInstanceOf[Lazy[O[A]]], Lazy(ask(suspend.value)))
          )
      }
    }
  }

  private def vectorTraverse[H[_], A, B](
      vfa: Vector[A]
  )(f: A => H[B])(implicit F: MonadThrowLike[H]): H[Vector[B]] = vfa match {
    case Vector() => F.pure(Vector())
    case head +: tail =>
      F.map2(f(head), vectorTraverse(tail)(f))((h, t) => t.prepended(h))
    case _ => ??? // The compiler thinks the above is not exhaustive(2.13.16)
  }

  private def vectorSequence[H[_], A](vfa: Vector[H[A]])(implicit
      F: MonadThrowLike[H]
  ): H[Vector[A]] = vectorTraverse(vfa)(identity)
}