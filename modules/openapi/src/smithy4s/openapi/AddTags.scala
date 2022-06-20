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

package smithy4s.openapi

import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.traits.TagsTrait
import software.amazon.smithy.model.traits.Trait
import software.amazon.smithy.openapi.fromsmithy.Context
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper
import software.amazon.smithy.openapi.model.OperationObject

import scala.jdk.CollectionConverters._

class AddTags() extends OpenApiMapper {

  override def postProcessOperation(
      context: Context[_ <: Trait],
      shape: OperationShape,
      operation: OperationObject,
      httpMethodName: String,
      path: String
  ): OperationObject = {
    val maybeTags = shape.getTrait(classOf[TagsTrait])
    val builder = operation.toBuilder()
    if (maybeTags.isPresent()) {
      maybeTags
        .get()
        .getValues()
        .asScala
        .foreach(builder.addTag)
    }
    builder.build()
  }

}
