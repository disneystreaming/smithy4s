package smithy4s.dynamic

case class DynamicOp[I, E, O, SI, SO](
    namespace: String,
    name: String,
    data: I
)
