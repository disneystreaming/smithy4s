package smithy4s.dynamic.internals

import smithy4s.ShapeId

private case class NoInputOp[I, E, O, SI, SO](
    id: ShapeId
)
