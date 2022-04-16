/*
 *  Copyright 2021 Disney Streaming
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

package smithy4s.codegen.v1compat

import software.amazon.smithy.build.ProjectionTransformer
import software.amazon.smithy.build.TransformContext
import software.amazon.smithy.model.traits.EnumTrait
import software.amazon.smithy.model.shapes.EnumShape
import java.util.Optional
import scala.jdk.CollectionConverters._
import software.amazon.smithy.model.Model

/**
  * This is here since the default behavior of smithy v2 with regard to enums is insufficient for our
  * use case. Specifically, smithy v2 will only transform an EnumTrait to an EnumShape IF it has a `name`
  * field defined already OR the value matches a certain regex pattern. We want to convert all EnumTraits
  * to EnumShapes, regardless of what the values are. We are able to do this since we already had logic
  * that used values as names in the generated code using a toCamelCase function. This transformation
  * preserves the same logic that existed before, thus causing no changes to the generated code.
  */
final class V1EnumTransformation extends ProjectionTransformer {
  def getName(): String = "V1EnumTransformation"

  @annotation.nowarn("msg=class EnumTrait in package traits is deprecated")
  def transform(x: TransformContext): Model = {
    val stringEnumShapes =
      x.getModel().getStringShapesWithTrait(classOf[EnumTrait]).asScala
    val enumShapes = stringEnumShapes.map { s =>
      val trt = s.getTrait(classOf[EnumTrait]).get()
      val builder = EnumShape.builder().id(s.toShapeId())
      trt.getValues.asScala.zipWithIndex.foreach { case (td, ind) =>
        val name =
          enumValueClassName(
            optionalToOption(td.getName()),
            td.getValue(),
            ind
          )
        builder.addMember(name, td.getValue())
      }
      builder.build()
    }
    val b = x.getModel().toBuilder()
    stringEnumShapes.foreach(ss => b.removeShape(ss.getId))
    enumShapes.foreach(b.addShape)
    b.build()
  }

  private def toCamelCase(value: String): String = {
    val (_, output) = value.foldLeft((false, "")) {
      case ((wasLastSkipped, str), c) =>
        if (c.isLetterOrDigit) {
          val newC =
            if (wasLastSkipped) c.toString.capitalize else c.toString
          (false, str + newC)
        } else {
          (true, str)
        }
    }
    output
  }

  private def enumValueClassName(
      name: Option[String],
      value: String,
      ordinal: Int
  ) = {
    name.getOrElse {
      val camel = toCamelCase(value).capitalize
      if (camel.nonEmpty) camel else "Value" + ordinal
    }

  }

  private def optionalToOption[A](opta: Optional[A]): Option[A] =
    if (opta.isPresent()) Some(opta.get) else None

}
