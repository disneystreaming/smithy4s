/*
 *  Copyright 2021-2022 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package smithy4s.codegen.transformers

import smithy4s.codegen.transformers.AwsStandardTypesTransformer.MemberShapeBuilderOps
import software.amazon.smithy.build.ProjectionTransformer
import software.amazon.smithy.build.TransformContext
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes._
import software.amazon.smithy.model.traits.BoxTrait
import software.amazon.smithy.model.traits.DefaultTrait
import software.amazon.smithy.model.traits.Trait

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
      .map[Shape] { shape =>
        val target = model.expectShape(shape.getTarget)
        val replacementShape = replaceWith(shape.getTarget)
        // Member shapes can only carry the `@default` trait IF
        // their container is a structures
        val canCarryDefault =
          model.expectShape(shape.getContainer()).isStructureShape()

        shape
          .toBuilder()
          .target(replacementShape)
          .when(canCarryDefault)(_.copyDefaultTrait(target))
          .build()
      }
      .orElse(shape)
  }

  private def isAwsShape(shape: Shape): Boolean =
    shape.getId.getNamespace.startsWith(awsNamespacePrefix)

  private def canReplace(shape: Shape): Boolean = {
    shape.isInstanceOf[SimpleShape] &&
    isAwsShape(shape) &&
    onlySupportedTraits(shape.getAllTraits) &&
    hasStandardName(shape)
  }

  @annotation.nowarn
  private def onlySupportedTraits(traits: java.util.Map[ShapeId, Trait]) =
    traits
      .values()
      .stream()
      .allMatch(t => {
        t.isInstanceOf[DefaultTrait] || t.isInstanceOf[BoxTrait]
      })

  private def hasStandardName(shape: Shape): Boolean =
    AwsStandardTypesTransformer.standardSimpleShapes.contains(
      shape.getId().getName()
    )

}

object AwsStandardTypesTransformer {

  val name: String = "AwsStandardTypesTransformer"

  val standardSimpleShapes = Set(
    "Integer",
    "Short",
    "Long",
    "Float",
    "Double",
    "BigInteger",
    "BigDecimal",
    "Byte",
    "Blob",
    "Boolean",
    "String",
    "Document",
    "Timestamp"
  )

  private[transformers] final implicit class MemberShapeBuilderOps(
      val builder: MemberShape.Builder
  ) extends AnyVal {
    def when(condition: Boolean)(
        mutation: MemberShape.Builder => MemberShape.Builder
    ): MemberShape.Builder =
      if (condition)(mutation(builder)) else builder

    def copyDefaultTrait(shape: Shape): MemberShape.Builder = {
      val defaultTraitOpt = toOption(shape.getTrait(classOf[DefaultTrait]))
      defaultTraitOpt.fold(builder)(builder.addTrait(_))
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
