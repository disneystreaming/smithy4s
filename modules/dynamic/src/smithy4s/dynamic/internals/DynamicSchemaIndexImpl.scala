package smithy4s
package dynamic
package internals

private[internals] class DynamicSchemaIndexImpl(
    serviceMap: Map[ShapeId, DynamicService],
    schemaMap: Map[ShapeId, Schema[DynData]]
) extends DynamicSchemaIndex {

  def allServices: List[DynamicSchemaIndex.ServiceWrapper] =
    serviceMap.values.toList

  def getSchema(
      shapeId: ShapeId
  ): Option[Schema[_]] =
    schemaMap.get(shapeId)

}
