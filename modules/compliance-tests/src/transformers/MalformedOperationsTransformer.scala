package transformers

import software.amazon.smithy.build.{ProjectionTransformer, TransformContext}
import software.amazon.smithy.model.Model

final class MalformedOperationsTransformer extends ProjectionTransformer {

  override def getName: String = "MalformedOperationsTransformer"

  def transform(ctx: TransformContext): Model = {
    ctx
      .getTransformer()
      .removeShapesIf(
        ctx.getModel(),
        shape => shape.getId.getName.startsWith("Malformed")
      )
  }

}
