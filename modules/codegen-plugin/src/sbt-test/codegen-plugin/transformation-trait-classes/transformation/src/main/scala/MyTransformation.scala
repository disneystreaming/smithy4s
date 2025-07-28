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
