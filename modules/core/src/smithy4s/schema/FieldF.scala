package smithy4s.schema

import smithy4s.Lazy

/**
 * A Field pattern functor.
 */
case class FieldF[S, A, T, I](
    label: String,
    schema: Lazy[T],
    get: S => A,
    in: I
)
