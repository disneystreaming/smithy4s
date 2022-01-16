package smithy4s
package dynamic

class DynamicModel(
    serviceMap: Map[ShapeId, DynamicService],
    schemaMap: Map[ShapeId, Schema[DynData]]
) {

  def allServices: List[DynamicModel.ServiceWrapper] =
    serviceMap.values.toList

  def getSchema(
      namespace: String,
      name: String
  ): Option[Schema[_]] = {
    val shapeId = ShapeId(namespace, name)
    schemaMap.get(shapeId)
  }
}

object DynamicModel {

  /**
    * A construct that hides the types a service instance works,
    * virtually turning them into existential types.
    *
    * This prevents the user from calling the algebra/transformation
    * in an unsafe fashion.
    */
  trait ServiceWrapper {
    type Alg[P[_, _, _, _, _]]
    type Op[I, E, O, SI, SO]

    def service: Service[Alg, Op]
  }

}
