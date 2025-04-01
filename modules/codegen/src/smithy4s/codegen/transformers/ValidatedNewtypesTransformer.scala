/*
 *  Copyright 2021-2025 Disney Streaming
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

import smithy4s.meta.UnwrapTrait
import smithy4s.meta.ValidateNewtypeTrait
import software.amazon.smithy.build.ProjectionTransformer
import software.amazon.smithy.build.TransformContext
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.AbstractShapeBuilder
import software.amazon.smithy.model.shapes.NumberShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.StringShape
import software.amazon.smithy.model.traits.LengthTrait
import software.amazon.smithy.model.traits.PatternTrait
import software.amazon.smithy.model.traits.RangeTrait

import scala.jdk.OptionConverters._
import smithy4s.codegen.CodegenRecord

class ValidatedNewtypesTransformer extends ProjectionTransformer {

  override def getName(): String = ValidatedNewtypesTransformer.name

  override def transform(context: TransformContext): Model = {
    val transformer = context.getTransformer()

    val model = context.getModel()
    val agn = CodegenRecord
      .recordsFromModel(model)
      .flatMap(record =>
        record.namespaces.map(ns =>
          (ns, record.validatedNewtypes.getOrElse(false))
        )
      )
      .toMap

    val supportsValidatedNewtypes =
      model
        .getMetadataProperty(ValidatedNewtypesTransformer.METADATA_KEY)
        .toScala
        .flatMap(_.asBooleanNode().toScala.map(_.getValue()))
        .getOrElse(false)

    transformer.mapShapes(
      model,
      s => processShape(s, agn.getOrElse(_, supportsValidatedNewtypes))
    )
  }

  private def processShape(shape: Shape, lookup: String => Boolean) =
    if (lookup(shape.getId().getNamespace()))
      shape match {
        case ValidatedNewtypesTransformer.SupportedShape(s) =>
          addTrait(Shape.shapeToBuilder(s): AbstractShapeBuilder[_, _])
        case _ => shape
      }
    else
      shape

  private def addTrait[S <: Shape, B <: AbstractShapeBuilder[B, S]](
      builder: AbstractShapeBuilder[B, S]
  ): S = {
    builder.addTrait(new ValidateNewtypeTrait())
    builder.build()
  }

}

object ValidatedNewtypesTransformer {
  val name = "ValidatedNewtypesTransformer"

  private val METADATA_KEY = "smithy4sRenderValidatedNewtypes"

  object SupportedShape {
    def unapply(shape: Shape): Option[Shape] = shape match {
      case _ if shape.hasTrait(classOf[UnwrapTrait])          => None
      case _ if shape.hasTrait(classOf[ValidateNewtypeTrait]) => None
      case s: StringShape if hasStringConstraints(s)          => Some(s)
      case n: NumberShape if hasNumberConstraints(n)          => Some(n)
      case _                                                  => None
    }

    private def hasStringConstraints(shape: Shape): Boolean =
      shape.getTrait(classOf[LengthTrait]).isPresent ||
        shape.getTrait(classOf[PatternTrait]).isPresent

    private def hasNumberConstraints(shape: Shape): Boolean =
      shape.getTrait(classOf[RangeTrait]).isPresent
  }
}
