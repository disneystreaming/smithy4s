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

import software.amazon.smithy.build.ProjectionTransformer
import software.amazon.smithy.build.TransformContext
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes._
import software.amazon.smithy.model.traits._

import java.util.function.Function
import java.util.ServiceLoader
import scala.jdk.CollectionConverters._
import software.amazon.smithy.model.node.Node

@annotation.nowarn("msg=class EnumTrait in package traits is deprecated")
private[codegen] final class OpenEnumTransformer extends ProjectionTransformer {
  override def getName: String = OpenEnumTransformer.name

  private val awsNamespacePrefix = "com.amazonaws"

  def transform(ctx: TransformContext): Model = {

    val loader =
      ServiceLoader.load(classOf[TraitService], getClass().getClassLoader())

    @annotation.nowarn("msg=method mapValues in trait MapOps is deprecated")
    val services = loader
      .iterator()
      .asScala
      .toList
      .groupBy(_.getShapeId())
      .mapValues(_.head)
      .toMap

    val openEnumId = ShapeId.from("alloy#openEnum")

    services.get(openEnumId) match {
      case Some(openEnumTraitService) =>
        val shapeMapper: Function[Shape, Shape] = { (shp: Shape) =>
          val openEnumTrait =
            openEnumTraitService.createTrait(openEnumId, Node.objectNode())
          shp match {
            case shp
                if !shp.getId.getNamespace.startsWith(awsNamespacePrefix) =>
              shp
            case e: EnumShape =>
              e.toBuilder.addTrait(openEnumTrait).build()
            case e: IntEnumShape =>
              e.toBuilder.addTrait(openEnumTrait).build()
            case t: Shape if t.hasTrait(classOf[EnumTrait]) =>
              (Shape
                .shapeToBuilder(t): AbstractShapeBuilder[_, _])
                .addTrait(openEnumTrait)
                .build()
            case other => other
          }
        }
        ctx.getTransformer().mapShapes(ctx.getModel(), shapeMapper)

      case None => ctx.getModel()
    }

  }

}

object OpenEnumTransformer {
  val name: String = "OpenEnumTransformer"
}
