package transformers

import software.amazon.smithy.build.{ProjectionTransformer, TransformContext}
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ShapeId

import java.util.{Map => JMap}
import scala.jdk.CollectionConverters.MapHasAsJava

final class ProtocolTransformer extends ProjectionTransformer {
  override def getName: String = "ProtocolTransformer"

  def transform(ctx: TransformContext): Model = {
    val rename: JMap[ShapeId, ShapeId] = Map(
      ShapeId.from("aws.protocols#restJson1") -> ShapeId.from(
        "alloy#simpleRestJson"
      )
    ).asJava
    ctx.getTransformer().renameShapes(ctx.getModel(), rename)
  }
}
