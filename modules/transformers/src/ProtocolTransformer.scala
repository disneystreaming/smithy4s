package smithy4s.transformers

import software.amazon.smithy.build.{ProjectionTransformer, TransformContext}
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ShapeId

import java.util.{HashMap => JHMap}

final class ProtocolTransformer extends ProjectionTransformer {
  override def getName: String = "ProtocolTransformer"

  def transform(ctx: TransformContext): Model = {
    val rename: JHMap[ShapeId, ShapeId] = {
      val map = new JHMap[ShapeId, ShapeId]()
      map.put(
        ShapeId.from("aws.protocols#restJson1"),
        ShapeId.from("alloy#simpleRestJson")
      )
      map
    }
    ctx.getTransformer().renameShapes(ctx.getModel(), rename)
  }
}
