package smithy4s.dynamic

import smithy4s.http.PayloadError
import software.amazon.smithy.model.shapes.ModelSerializer

private[dynamic] trait DynamicSchemaIndexPlatform {
  self: DynamicSchemaIndex.type =>

  /**
    * Loads a dynamic schema index model from a smithy model.
    */
  def loadModel(
      model: software.amazon.smithy.model.Model
  ): Either[PayloadError, DynamicSchemaIndex] = {
    val node = ModelSerializer.builder().build.serialize(model)
    val document = NodeToDocument(node)
    smithy4s.Document
      .decode[smithy4s.dynamic.model.Model](document)
      .map(load(_))
  }

}
