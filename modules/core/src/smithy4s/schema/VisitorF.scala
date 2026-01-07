package smithy4s.schema

import smithy4s.capability.{Cache, MonadThrowLike}
import smithy4s._

/**
 * An effectful visitor for schemas.
 */
trait VisitorF[G[_]] {
  def primitive[P](shapeId: ShapeId, hints: Hints, tag: Primitive[P]): G[P]
  def collection[C[_], A](
                           shapeId: ShapeId,
                           hints: Hints,
                           tag: CollectionTag[C],
                           member: Lazy[G[A]]
                         ): G[C[A]]
  def map[C[_, _], K, V](
                 shapeId: ShapeId,
                 hints: Hints,
                 tag: MapTag[C],
                 key: Lazy[G[K]],
                 value: Lazy[G[V]]
               ): G[C[K, V]]
  def enumeration[E](
                      shapeId: ShapeId,
                      hints: Hints,
                      tag: EnumTag[E],
                      values: List[EnumValue[E]]
                    ): G[E]
  def struct[S](
                 shapeId: ShapeId,
                 hints: Hints,
                 fields: Vector[FieldF[S, _, G[Any]]],
                 make: IndexedSeq[Any] => S
               ): G[S]
  def union[U](
                shapeId: ShapeId,
                hints: Hints,
                alternatives: Vector[AltF[U, _, G[Any]]],
                dispatch: U => Int
              ): G[U]
  def biject[A, B](schema: Lazy[G[A]], bijection: Bijection[A, B]): G[B]
  def refine[A, B](schema: Lazy[G[A]], refinement: Refinement[A, B]): G[B]
  def lazily[A](suspend: Lazy[G[A]]): G[A]
  def option[C[_], A](tag: OptionalTag[C], schema: Lazy[G[A]]): G[C[A]]

  final def apply[F[_], A](schema: Schema[A], cache: Cache[F, G[_]])(implicit F: MonadThrowLike[F]): F[G[A]] = {
    schema match {
      case Schema.PrimitiveSchema(shapeId, hints, tag) =>
        F.pure(primitive(shapeId, hints, tag))
      case Schema.CollectionSchema(shapeId, hints, tag, member) =>
        F.map(cache.get(member))(memberG =>
          collection(shapeId, hints, tag, memberG.asInstanceOf[Lazy[G[Any]]])
        )
      case Schema.MapSchema(shapeId, hints, tag, key, value) =>
        F.flatMap(cache.get(key))(keyG =>
          F.map(cache.get(value))(valueG => map(shapeId, hints, tag, keyG.asInstanceOf[Lazy[G[Any]]], valueG.asInstanceOf[Lazy[G[Any]]]))
        )
      case Schema.EnumerationSchema(shapeId, hints, tag, values) =>
        F.pure(enumeration(shapeId, hints, tag, values))
      case Schema.StructSchema(shapeId, hints, fields, make) =>
        val fieldFGs = fields.map { field =>
          F.map(cache.get(field.schema))(fieldG =>
            FieldF[A, Any, G[Any]](field.label, fieldG.asInstanceOf[Lazy[G[Any]]], field.get)
          )
        }
        F.map(vectorSequence(fieldFGs))(fieldGs =>
          struct(shapeId, hints, fieldGs, make)
        )
      case Schema.UnionSchema(shapeId, hints, alternatives, ordinal) =>
        val altFGs = alternatives.map(alt =>
          F.map(cache.get(alt.schema))(altF =>
            AltF[A, Any, G[Any]](alt.label, altF.asInstanceOf[Lazy[G[Any]]], alt.inject.asInstanceOf[Any => A], alt.project)
          )
        )
        F.map(vectorSequence(altFGs))(altGs =>
          union(shapeId, hints, altGs, ordinal)
        )
      case Schema.OptionSchema(tag, underlying) =>
        F.map(cache.get(underlying))(underG => option(tag, underG.asInstanceOf[Lazy[G[Any]]]))
      case Schema.BijectionSchema(underlying, bijection) =>
        F.map(cache.get(underlying))(underG => biject(underG.asInstanceOf[Lazy[G[Any]]], bijection))
      case Schema.RefinementSchema(underlying, refinement) =>
        F.map(cache.get(underlying))(underG => refine(underG.asInstanceOf[Lazy[G[Any]]], refinement))
      case Schema.LazySchema(suspend) =>
        F.map(cache.get(suspend.value))(susG => lazily(susG.asInstanceOf[Lazy[G[A]]]))
    }
  }

  private def vectorTraverse[H[_], A, B](
      vfa: Vector[A]
  )(f: A => H[B])(implicit F: MonadThrowLike[H]): H[Vector[B]] = vfa match {
      case Vector() => F.pure(Vector())
      case head +: tail =>
        F.map2(f(head), vectorTraverse(tail)(f))((h, t) => t.prepended(h))
      case _ => ??? //The compiler thinks the above is not exhaustive(2.13.16)
    }


  private def vectorSequence[H[_], A](vfa: Vector[H[A]])(implicit
      F: MonadThrowLike[H]
  ): H[Vector[A]] = vectorTraverse(vfa)(identity)
}