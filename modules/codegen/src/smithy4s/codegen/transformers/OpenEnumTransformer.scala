/*
 *  Copyright 2021-2024 Disney Streaming
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

import alloy.OpenEnumTrait
import software.amazon.smithy.build.{ProjectionTransformer, TransformContext}
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes._
import software.amazon.smithy.model.traits._
import java.util.function.Function

@annotation.nowarn("msg=class EnumTrait in package traits is deprecated")
private[codegen] final class OpenEnumTransformer extends ProjectionTransformer {
  override def getName: String = OpenEnumTransformer.name

  private val awsNamespacePrefix = "com.amazonaws"

  def transform(ctx: TransformContext): Model = {
    val shapeMapper: Function[Shape, Shape] = { (shp: Shape) =>
      shp match {
        case shp if !shp.getId.getNamespace.startsWith(awsNamespacePrefix) =>
          shp
        case e: EnumShape =>
          e.toBuilder.addTrait(new OpenEnumTrait()).build()
        case e: IntEnumShape =>
          e.toBuilder.addTrait(new OpenEnumTrait()).build()
        case t: Shape if t.hasTrait(classOf[EnumTrait]) =>
          (Shape
            .shapeToBuilder(t): AbstractShapeBuilder[_, _])
            .addTrait(new OpenEnumTrait())
            .build()
        case other => other
      }
    }
    ctx.getTransformer().mapShapes(ctx.getModel(), shapeMapper)
  }

}

object OpenEnumTransformer {
  val name: String = "OpenEnumTransformer"
}
