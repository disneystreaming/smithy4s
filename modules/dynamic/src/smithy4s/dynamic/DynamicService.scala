package smithy4s
package dynamic

case class DynamicService(
    id: ShapeId,
    version: String,
    getEndpoints: () => List[DynamicEndpoint],
    hints: Hints
) extends Service[DynamicAlg, DynamicOp]
    with DynamicModel.ServiceWrapper {

  type Alg[P[_, _, _, _, _]] = DynamicAlg[P]
  type Op[I, E, O, SI, SO] = DynamicOp[I, E, O, SI, SO]
  override val service: Service[Alg, Op] = this

  def transform[F[_, _, _, _, _], G[_, _, _, _, _]](
      alg: DynamicAlg[F],
      transformation: Transformation[F, G]
  ): DynamicAlg[G] = alg.andThen(transformation)

  lazy val endpoints: List[Endpoint[DynamicOp, _, _, _, _, _]] =
    getEndpoints()

  private lazy val endpointMap
      : Map[ShapeId, Endpoint[DynamicOp, _, _, _, _, _]] =
    endpoints.map(ep => ep.id -> ep).toMap

  def endpoint[I, E, O, SI, SO](
      op: DynamicOp[I, E, O, SI, SO]
  ): (I, Endpoint[DynamicOp, I, E, O, SI, SO]) = {
    val endpoint = endpointMap
      .get(op.id)
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
