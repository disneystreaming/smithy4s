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
import smithy4s.api.UntaggedUnionTrait

import scala.jdk.CollectionConverters._

class UntaggedUnions() extends JsonSchemaMapper {
  private final val COMPONENTS = "components"

  def updateSchema(
      shape: Shape,
      schemaBuilder: Builder,
      config: JsonSchemaConfig
  ): Builder = if (shape.hasTrait(classOf[UntaggedUnionTrait])) {
    val unionSchema = schemaBuilder.build()

    val alternatives = unionSchema.getOneOf().asScala
    val untaggedAlts = alternatives.map { alt =>
      val untaggedAlt =
        alt
          .getProperties()
          .asScala
          .head // each alternative is an object with a single property
          ._2

      if (!untaggedAlt.getRef().isPresent()) {
        // If the alternative is not just referencing another model,
        // we annotate it with the tagged title.
        untaggedAlt
          .toBuilder()
          .title(alt.getTitle().get())
          .build()
      } else untaggedAlt
    }
    schemaBuilder.oneOf(untaggedAlts.asJava)
  } else schemaBuilder
}
