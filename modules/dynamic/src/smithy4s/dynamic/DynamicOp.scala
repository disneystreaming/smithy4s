package smithy4s.dynamic

import smithy4s.ShapeId

case class DynamicOp[I, E, O, SI, SO](
    id: ShapeId,
    data: I
)
