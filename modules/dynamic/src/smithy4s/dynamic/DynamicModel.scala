package smithy4s
package dynamic

class DynamicModel(
    serviceMap: Map[ShapeId, DynamicService],
    schemaMap: Map[ShapeId, Schema[DynData]]
) {

  def allServices: List[DynamicService] = serviceMap.values.toList

  def getSchema(
      namespace: String,
      name: String
  ): Option[Schema[DynData]] = {
    val shapeId = ShapeId(namespace, name)
    schemaMap.get(shapeId)
  }
}
