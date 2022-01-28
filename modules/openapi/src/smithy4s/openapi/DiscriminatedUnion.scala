package smithy4s.openapi

import _root_.software.amazon.smithy.jsonschema.JsonSchemaConfig
import _root_.software.amazon.smithy.jsonschema.JsonSchemaMapper
import _root_.software.amazon.smithy.jsonschema.Schema.Builder
import _root_.software.amazon.smithy.model.shapes.Shape

import scala.jdk.CollectionConverters._
import software.amazon.smithy.model.node.ObjectNode
import smithy4s.api.DiscriminatedUnionTrait

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
      val unionSchema = schemaBuilder.build()

      val alternatives = unionSchema.getOneOf().asScala
      val discriminatedAlts = alternatives.flatMap(
        _.getProperties().asScala.map(_._2)
      )

      schemaBuilder
        .oneOf(discriminatedAlts.asJava)
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
