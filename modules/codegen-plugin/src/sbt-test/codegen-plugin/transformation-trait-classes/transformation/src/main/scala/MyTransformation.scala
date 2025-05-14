import software.amazon.smithy.build.ProjectionTransformer
import software.amazon.smithy.build.TransformContext
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.transform.ModelTransformer
import java.util.function.BiFunction
import software.amazon.smithy.model.traits.Trait
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.traits.DocumentationTrait
import software.amazon.smithy.model.shapes.ShapeId
import bsp.traits.DataTrait

class MyTransformation extends ProjectionTransformer {
  def getName(): String = "my-transformation"

  // Replace traits#jsonRPC with documentation
  def transform(context: TransformContext): Model = {
    // this would fail if the class wasn't present on the classpath.

    // external shape - regression test for #336
    context
      .getModel()
      .expectShape(ShapeId.from("bsp#BuildTargetData"))
      .expectTrait(classOf[DataTrait])

    // local shape
    context
      .getModel()
      .expectShape(ShapeId.from("my.input#MyShape"))
      .expectTrait(classOf[DataTrait])

    ModelTransformer
      .create()
      .mapTraits(
        context.getModel(),
        {
          case (_, _: DataTrait) =>
            new DocumentationTrait("what's up doc")
          case (_, trt) => trt
        }: BiFunction[Shape, Trait, Trait]
      )
  }

}
