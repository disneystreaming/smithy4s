package smithy4s
package dynamic

case class DynamicService(
    namespace: String,
    name: String,
    version: String,
    getEndpoints: () => List[DynamicEndpoint],
    hints: Hints
) extends Service[DynamicAlg, DynamicOp] {

  def transform[F[_, _, _, _, _], G[_, _, _, _, _]](
      alg: DynamicAlg[F],
      transformation: Transformation[F, G]
  ): DynamicAlg[G] = alg.andThen(transformation)

  def endpoints: List[Endpoint[DynamicOp, _, _, _, _, _]] = getEndpoints()

  def endpoint[I, E, O, SI, SO](
      op: DynamicOp[I, E, O, SI, SO]
  ): (I, Endpoint[DynamicOp, I, E, O, SI, SO]) = {
    val endpoint = endpoints
      .find(ep => ep.name == op.name)
      .get
      .asInstanceOf[Endpoint[DynamicOp, I, E, O, SI, SO]]
    val input = op.data
    (input, endpoint)
  }

  def transform[P[_, _, _, _, _]](
      transformation: Transformation[DynamicOp, P]
  ): DynamicAlg[P] = transformation

  def asTransformation[P[_, _, _, _, _]](
      impl: DynamicAlg[P]
  ): Transformation[DynamicOp, P] = impl
}
