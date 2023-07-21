package smithy4s.codegen.transformers

import smithy4s.codegen.transformers.AwsStandardTypesTransformer.MemberShapeBuilderOps
import software.amazon.smithy.build.{ProjectionTransformer, TransformContext}
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes._
import software.amazon.smithy.model.traits.{BoxTrait, DefaultTrait, Trait}

class AwsStandardTypesTransformer extends ProjectionTransformer {

  private val smithyStandardNamespace = "smithy.api"
  private val awsNamespacePrefix = "com.amazonaws"

  override def getName: String = AwsStandardTypesTransformer.name

  override def transform(context: TransformContext): Model = {
    val transformer = context.getTransformer

    transformer.removeShapesIf(
      transformer.mapShapes(context.getModel, mapFunction(context.getModel)),
      canReplace
    )
  }

  private def mapFunction(
      model: Model
  ): java.util.function.Function[Shape, Shape] = shape => {
    def replaceWith(shapeId: ShapeId): ShapeId = {
      val shapeType =
        model
          .expectShape(
            shapeId
          ) // this should be safe as we already did some filtering before calling the method
          .getType

      // Use the shape type instead of relying on the name
      // because we want to be able to replace something
      // com.amazonaws.dynamodb#Date with smithy.api#Timestamp
      val shapeName = shapeType.getShapeClass.getSimpleName.replace("Shape", "")

      ShapeId.fromParts(smithyStandardNamespace, shapeName)
    }

    shape
      .asMemberShape()
      .filter { memberShape =>
        model
          .getShape(memberShape.getTarget)
          .map[Boolean](canReplace)
          .orElse(false)
      }
      .map[Shape](shape => {
        val target = model.expectShape(shape.getTarget)
        val replacementShape = replaceWith(shape.getTarget)

        shape
          .toBuilder()
          .target(replacementShape)
          .copyDefaultTrait(target)
          .build()
      })
      .orElse(shape)
  }

  private def isAwsShape(shape: Shape): Boolean =
    shape.getId.getNamespace.startsWith(awsNamespacePrefix)

  private def canReplace(shape: Shape): Boolean = {
    shape.isInstanceOf[SimpleShape] &&
    isAwsShape(shape) &&
    onlySupportedTraits(shape.getAllTraits)
  }

  @annotation.nowarn
  private def onlySupportedTraits(traits: java.util.Map[ShapeId, Trait]) =
    traits
      .values()
      .stream()
      .allMatch(t => {
        t.isInstanceOf[DefaultTrait] || t.isInstanceOf[BoxTrait]
      })

}

object AwsStandardTypesTransformer {

  val name: String = "AwsStandardTypesTransformer"

  private[transformers] final implicit class MemberShapeBuilderOps(
      val builder: MemberShape.Builder
  ) extends AnyVal {
    def copyDefaultTrait(shape: Shape): MemberShape.Builder = {
      val defaultTraitOpt = toOption(shape.getTrait(classOf[DefaultTrait]))

      defaultTraitOpt
        .fold(builder)(builder.addTrait(_))
    }
  }

  private def toOption[A](o: java.util.Optional[A]): Option[A] = {
    if (o.isPresent) {
      Some(o.get())
    } else {
      None
    }
  }
}
