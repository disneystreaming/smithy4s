package smithy4s.schema

import smithy4s.Lazy

/**
 * An alternative pattern functor. 
 */
case class AltF[U, A, T](
    label: String,
    schema: Lazy[T],
    inject: A => U,
    project: PartialFunction[U, A]
)
