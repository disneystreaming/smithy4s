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

package smithy4s.openapi

import _root_.software.amazon.smithy.jsonschema.JsonSchemaConfig
import _root_.software.amazon.smithy.jsonschema.JsonSchemaMapper
import _root_.software.amazon.smithy.jsonschema.Schema.Builder
import _root_.software.amazon.smithy.model.shapes.Shape
import smithy4s.api.DiscriminatedUnionTrait
import software.amazon.smithy.jsonschema.Schema
import software.amazon.smithy.model.node.ObjectNode

import scala.jdk.CollectionConverters._

class DiscriminatedUnions() extends JsonSchemaMapper {
  private final val COMPONENTS = "components"

  def updateSchema(
      shape: Shape,
      schemaBuilder: Builder,
      config: JsonSchemaConfig
  ): Builder = {
    val maybeDiscriminated = shape.getTrait(classOf[DiscriminatedUnionTrait])
    if (maybeDiscriminated.isPresent()) {
      val discriminated = maybeDiscriminated.get()
      val discriminatorField = discriminated.getValue()
      val unionSchema = schemaBuilder.build()

      val alternatives = unionSchema.getOneOf().asScala
      val discriminatedAlts =
        alternatives.flatMap(alt => alt.getProperties().asScala)

      val transformedAlts = discriminatedAlts.map { case (label, altSchema) =>
        val localDiscriminator = Schema
          .builder()
          .`type`("string")
          .enumValues(List(label).asJava)
          .build()
        val discObject = Schema
          .builder()
          .`type`("object")
          .properties(
            Map(
              discriminatorField -> localDiscriminator
            ).asJava
          )
          .required(List(discriminatorField).asJava)
          .build()
        Schema
          .builder()
          .allOf(
            List(altSchema, discObject).asJava
          )
          .build()
      }.asJava

      schemaBuilder
        .oneOf(transformedAlts)
        .putExtension(
          "discriminator",
          ObjectNode
            .builder()
            .withMember("propertyName", discriminated.getValue())
            .build()
        )

    } else schemaBuilder
  }
}
