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

package software.amazon.smithy.openapi.fromsmithy.protocols

import cats.syntax.all._
import smithy4s.api.UncheckedExamplesTrait
import software.amazon.smithy.jsonschema.Schema
import software.amazon.smithy.model.knowledge.HttpBinding.Location
import software.amazon.smithy.model.knowledge._
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes._
import software.amazon.smithy.model.traits._
import software.amazon.smithy.openapi.OpenApiException
import software.amazon.smithy.openapi.fromsmithy.Context
import software.amazon.smithy.openapi.fromsmithy.OpenApiProtocol
import software.amazon.smithy.openapi.fromsmithy.OpenApiProtocol.Operation
import software.amazon.smithy.openapi.model._

import java.util
import java.util.function.Function
import scala.jdk.CollectionConverters._

/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

/**
  * Provides the shared functionality used across protocols that use Smithy's
  * HTTP binding traits.
  *
  * <p>This class handles adding query string, path, header, payload, and
  * document bodies to HTTP messages using an {@link HttpBindingIndex}.
  * Inline schemas as created for query string, headers, and path
  * parameters that do not utilize the correct types or set an explicit
  * type/format (for example, this class ensures that a timestamp shape
  * serialized in the query string is serialized using the date-time
  * format).
  *
  * <p>This class is currently package-private, but may be made public in the
  * future when we're sure about its API.
  */
object Smithy4sAbstractRestProtocol {

  sealed trait MessageType
  object MessageType {
    case object REQUEST extends MessageType
    case object RESPONSE extends MessageType
    case object ERROR extends MessageType
  }

}

abstract class Smithy4sAbstractRestProtocol[T <: Trait]
    extends OpenApiProtocol[T] {

  /**
    * Gets the media type of a document sent in a request or response.
    */
  def getDocumentMediaType(): String

  /**
    * Creates a schema to send a document payload in the request,
    * response, or error of an operation.
    *
    * @param context          Conversion context.
    * @param operationOrError Operation shape or error shape.
    * @param bindings         HTTP bindings of this shape.
    * @param messageType      The message type (request, response, or error).
    * @return Returns the created document schema.
    */
  def createDocumentSchema(
      context: Context[T],
      operationOrError: Shape,
      bindings: List[HttpBinding],
      messageType: Smithy4sAbstractRestProtocol.MessageType
  ): Schema

  override def createOperation(context: Context[T], operation: OperationShape) =
    operation
      .getTrait(classOf[HttpTrait])
      .asScala
      .map((httpTrait: HttpTrait) => {
        val method =
          context.getOpenApiProtocol.getOperationMethod(context, operation)
        val uri =
          context.getOpenApiProtocol.getOperationUri(context, operation)
        val builder =
          OperationObject.builder.operationId(operation.getId.getName)
        val bindingIndex = HttpBindingIndex.of(context.getModel)
        val eventStreamIndex = EventStreamIndex.of(context.getModel)
        createPathParameters(context, operation).foreach(builder.addParameter)
        createQueryParameters(context, operation).foreach(builder.addParameter)
        createRequestHeaderParameters(context, operation)
          .foreach(builder.addParameter)
        createRequestBody(context, bindingIndex, eventStreamIndex, operation)
          .foreach(builder.requestBody)
        createResponses(context, bindingIndex, eventStreamIndex, operation)
          .foreach { case (k, v) => builder.putResponse(k, v) }
        Operation.create(method, uri, builder)
      })
      .asJava

  def createPathParameters(
      context: Context[T],
      operation: OperationShape
  ) = {
    val bindingIndex = HttpBindingIndex.of(context.getModel)
    val httpTrait = operation.expectTrait(classOf[HttpTrait])

    for (
      binding <- bindingIndex
        .getRequestBindings(
          operation,
          HttpBinding.Location.LABEL
        )
        .asScala
    ) yield {
      val schema = createPathParameterSchema(context, binding)
      val memberName = binding.getMemberName
      val label = httpTrait.getUri
        .getLabel(memberName)
        .orElseThrow(() =>
          new OpenApiException(
            String.format(
              "Unable to find URI label on %s for %s: %s",
              operation.getId,
              binding.getMemberName,
              httpTrait.getUri
            )
          )
        )
      // Greedy labels in OpenAPI need to include the label in the generated parameter.
      // For example, given "/{foo+}", the parameter name must be "foo+".
      // Some vendors/tooling, require the "+" suffix be excluded in the generated parameter.
      // If required, the setRemoveGreedyParameterSuffix config option should be set to `true`.
      // When this option is enabled, given "/{foo+}", the parameter name will be "foo".
      var name = label.getContent
      if (
        label.isGreedyLabel && !context.getConfig.getRemoveGreedyParameterSuffix
      ) name = name + "+"

      val builder = ModelUtils
        .createParameterMember(context, binding.getMember)
        .name(name)
        .in("path")
        .schema(schema)

      createInputExamples(operation, memberName).foreach(builder.examples)

      builder.build
    }
  }

  private def createPathParameterSchema(
      context: Context[T],
      binding: HttpBinding
  ) = {
    val member = binding.getMember
    // Timestamps sent in the URI are serialized as a date-time string by default.
    if (needsInlineTimestampSchema(context, member)) { // Create a copy of the targeted schema and remove any possible numeric keywords.
      val copiedBuilder = ModelUtils.convertSchemaToStringBuilder(
        context.getSchema(context.getPointer(member))
      )
      copiedBuilder.format("date-time").build
    } else if (context.getJsonSchemaConverter.isInlined(member))
      context.getJsonSchemaConverter.convertShape(member).getRootSchema
    else context.createRef(binding.getMember)
  }

  private def needsInlineTimestampSchema(
      context: Context[_ <: Trait],
      member: MemberShape
  ): Boolean = {
    if (
      member
        .getMemberTrait(context.getModel, classOf[TimestampFormatTrait])
        .isPresent
    ) return false
    context.getModel
      .getShape(member.getTarget)
      .filter(_.isTimestampShape)
      .isPresent
  }

  // Creates parameters that appear in the query string. Each input member
  // bound to the QUERY location will generate a new ParameterObject that
  // has a location of "query".
  private def createQueryParameters(
      context: Context[T],
      operation: OperationShape
  ) = {
    val httpBindingIndex = HttpBindingIndex.of(context.getModel)
    for (
      binding <- httpBindingIndex
        .getRequestBindings(
          operation,
          HttpBinding.Location.QUERY
        )
        .asScala
    ) yield {
      val member = binding.getMember
      val param = ModelUtils
        .createParameterMember(context, member)
        .in("query")
        .name(binding.getLocationName)
      val target = context.getModel.expectShape(member.getTarget)
      // List and set shapes in the query string are repeated, so we need to "explode" them
      // using the "form" style (e.g., "foo=bar&foo=baz").
      // See https://swagger.io/specification/#style-examples
      if (target.isInstanceOf[CollectionShape])
        param.style("form").explode(true)
      // Create the appropriate schema based on the shape type.
      val refSchema = context.inlineOrReferenceSchema(member)
      val visitor = new QuerySchemaVisitor[T](context, refSchema, member)
      param.schema(target.accept(visitor))
      createInputExamples(operation, binding.getMemberName).foreach(
        param.examples
      )

      param.build
    }
  }

  private def createRequestHeaderParameters(
      context: Context[T],
      operation: OperationShape
  ) = {
    val bindings = HttpBindingIndex
      .of(context.getModel)
      .getRequestBindings(operation, HttpBinding.Location.HEADER)
    createHeaderParameters(
      context,
      bindings,
      operation,
      AbstractRestProtocol.MessageType.REQUEST
    ).values
  }

  private def createHeaderParameters(
      context: Context[T],
      bindings: util.List[HttpBinding],
      operation: Shape,
      messageType: AbstractRestProtocol.MessageType
  ) = {
    val result = for (binding <- bindings.asScala) yield {
      val member = binding.getMember
      val param = ModelUtils.createParameterMember(context, member)
      if (messageType eq AbstractRestProtocol.MessageType.REQUEST) {
        param.in("header").name(binding.getLocationName)
        createInputExamples(operation, binding.getMemberName)
          .foreach(param.examples)
      } else { // Response headers don't use "in" or "name".
        param.in(null).name(null)
        createOutputExamples(operation, binding.getMemberName)
          .foreach(param.examples)
      }
      val target = context.getModel.expectShape(member.getTarget)
      val refSchema = context.inlineOrReferenceSchema(member)
      val visitor = new HeaderSchemaVisitor[T](context, refSchema, member)
      param.schema(target.accept(visitor))
      binding.getLocationName -> param.build
    }
    result.toMap
  }

  private def createRequestBody(
      context: Context[T],
      bindingIndex: HttpBindingIndex,
      eventStreamIndex: EventStreamIndex,
      operation: OperationShape
  ) = {
    val payloadBindings =
      bindingIndex.getRequestBindings(operation, HttpBinding.Location.PAYLOAD)
    // Get the default media type if one cannot be resolved.
    val mediaType =
      determineContentType(
        bindingIndex.getRequestBindings(operation).values().asScala
      )
    if (payloadBindings.isEmpty)
      createRequestDocument(mediaType, context, bindingIndex, operation)
    else
      createRequestPayload(
        mediaType,
        context,
        payloadBindings.get(0),
        operation
      )
  }

  private def createRequestPayload(
      mediaTypeRange: Option[String],
      context: Context[T],
      binding: HttpBinding,
      operation: OperationShape
  ) = { // API Gateway validation requires that in-line schemas must be objects
    // or arrays. These schemas are synthesized as references so that
    // any schemas with string types will pass validation.
    val schema = context.inlineOrReferenceSchema(binding.getMember)
    val mediaTypeObject = getMediaTypeObject(
      context,
      schema,
      operation,
      (shape: Shape) => {
        val shapeName = shape.getId.getName
        shapeName + "InputPayload"
      }
    )
    val mtr = mediaTypeRange.getOrElse(getDocumentMediaType())

    val updatedMtObject = createInputExamples(operation, binding.getMemberName)
      .map(mediaTypeObject.toBuilder.examples(_).build)
      .getOrElse(mediaTypeObject)

    val requestBodyObject = RequestBodyObject.builder
      .putContent(mtr, updatedMtObject)
      .required(binding.getMember.isRequired)
      .build
    Some(requestBodyObject)
  }

  private def createRequestDocument(
      mediaType: Option[String],
      context: Context[T],
      bindingIndex: HttpBindingIndex,
      operation: OperationShape
  ): Option[RequestBodyObject] = {
    val bindings =
      bindingIndex.getRequestBindings(operation, HttpBinding.Location.DOCUMENT)
    // If nothing is bound to the document, then no schema needs to be synthesized.
    if (bindings.isEmpty) None
    else {
      // Synthesize a schema for the body of the request.
      val schema = createDocumentSchema(
        context,
        operation,
        bindings.asScala.toList,
        Smithy4sAbstractRestProtocol.MessageType.REQUEST
      )
      val synthesizedName = operation.getId.getName + "RequestContent"
      val pointer = context.putSynthesizedSchema(synthesizedName, schema)

      val memberNames = bindings.asScala.toList.map(_.getMemberName)
      val maybeExamples =
        createExamples(operation)(ExampleNode.forInputMembers(_, memberNames))
      val builder =
        MediaTypeObject.builder.schema(Schema.builder.ref(pointer).build)
      maybeExamples.foreach(builder.examples)
      val mediaTypeObject = builder.build
      // If any of the top level bindings are required, then the body itself must be required.
      val required = bindings.asScala.exists(_.getMember.isRequired)
      Some(
        RequestBodyObject.builder
          .putContent(getDocumentMediaType(), mediaTypeObject)
          .required(required)
          .build
      )
    }
  }

  private def createResponses(
      context: Context[T],
      bindingIndex: HttpBindingIndex,
      eventStreamIndex: EventStreamIndex,
      operation: OperationShape
  ) = {
    // Hack to ensure that the model contains the potentially updated
    // operation shape.
    val updatedModel =
      context.getModel().toBuilder().addShape(operation).build()
    val result = new util.TreeMap[String, ResponseObject]
    val operationIndex = OperationIndex.of(updatedModel)
    operationIndex
      .getOutputShape(operation)
      .asScala
      .foreach((output: StructureShape) => {
        updateResponsesMapWithResponseStatusAndObject(
          context,
          bindingIndex,
          eventStreamIndex,
          operation,
          output,
          result
        )
      })
    for (error <- operationIndex.getErrors(operation).asScala) {
      updateResponsesMapWithResponseStatusAndObject(
        context,
        bindingIndex,
        eventStreamIndex,
        operation,
        error,
        result
      )
    }
    result.asScala
  }

  private def updateResponsesMapWithResponseStatusAndObject(
      context: Context[T],
      bindingIndex: HttpBindingIndex,
      eventStreamIndex: EventStreamIndex,
      operation: OperationShape,
      shape: StructureShape,
      responses: util.Map[String, ResponseObject]
  ) = {
    val operationOrError =
      if (shape.hasTrait(classOf[ErrorTrait])) shape
      else operation
    val statusCode = context.getOpenApiProtocol.getOperationResponseStatusCode(
      context,
      operationOrError
    )
    val response = createResponse(
      context,
      bindingIndex,
      eventStreamIndex,
      statusCode,
      operationOrError
    )
    responses.put(statusCode, response)
  }

  private def createResponse(
      context: Context[T],
      bindingIndex: HttpBindingIndex,
      eventStreamIndex: EventStreamIndex,
      statusCode: String,
      operationOrError: Shape
  ) = {
    val responseBuilder = ResponseObject.builder
    responseBuilder.description(
      String.format(
        "%s %s response",
        operationOrError.getId.getName,
        statusCode
      )
    )
    createResponseHeaderParameters(context, operationOrError).foreach {
      case (k: String, v: ParameterObject) =>
        responseBuilder.putHeader(k, Ref.local(v))
    }
    addResponseContent(
      context,
      bindingIndex,
      eventStreamIndex,
      responseBuilder,
      statusCode,
      operationOrError
    )
    responseBuilder.build
  }

  private def createResponseHeaderParameters(
      context: Context[T],
      operationOrError: Shape
  ) = {
    val bindings = HttpBindingIndex
      .of(context.getModel)
      .getResponseBindings(operationOrError, HttpBinding.Location.HEADER)
    createHeaderParameters(
      context,
      bindings,
      operationOrError,
      AbstractRestProtocol.MessageType.RESPONSE
    )
  }

  private def addResponseContent(
      context: Context[T],
      bindingIndex: HttpBindingIndex,
      eventStreamIndex: EventStreamIndex,
      responseBuilder: ResponseObject.Builder,
      statusCode: String,
      operationOrError: Shape
  ) = {
    val payloadBindings = bindingIndex.getResponseBindings(
      operationOrError,
      HttpBinding.Location.PAYLOAD
    )
    val mediaType = determineContentType(
      bindingIndex.getResponseBindings(operationOrError).values().asScala
    )
    if (!payloadBindings.isEmpty)
      createResponsePayload(
        mediaType,
        context,
        payloadBindings.get(0),
        responseBuilder,
        operationOrError
      )
    else
      createResponseDocumentIfNeeded(
        getDocumentMediaType(),
        context,
        bindingIndex,
        responseBuilder,
        operationOrError
      )
  }

  private def createResponsePayload(
      mediaType: Option[String],
      context: Context[T],
      binding: HttpBinding,
      responseBuilder: ResponseObject.Builder,
      operationOrError: Shape
  ) = {
    val schema = context.inlineOrReferenceSchema(binding.getMember)
    val mediaTypeObject = getMediaTypeObject(
      context,
      schema,
      operationOrError,
      (shape: Shape) => {
        val shapeName = shape.getId.getName
        if (shape.isInstanceOf[OperationShape]) shapeName + "OutputPayload"
        else shapeName + "ErrorPayload"
      }
    )

    val updatedMtObject =
      createOutputExamples(operationOrError, binding.getMemberName)
        .map(mediaTypeObject.toBuilder.examples(_).build)
        .getOrElse(mediaTypeObject)

    mediaType.foreach { mt =>
      responseBuilder.putContent(mt, updatedMtObject)
    }
  }

  // If a synthetic schema is just a wrapper for another schema, create the
  // MediaTypeObject using the pointer to the existing schema, otherwise add
  // the synthetic schema and create the MediaTypeObject using a new pointer.
  private def getMediaTypeObject(
      context: Context[T],
      schema: Schema,
      shape: Shape,
      createSynthesizedName: Function[Shape, String]
  ) = if (!schema.getType.isPresent && schema.getRef.isPresent)
    MediaTypeObject.builder
      .schema(Schema.builder.ref(schema.getRef.get).build)
      .build
  else {
    val synthesizedName = createSynthesizedName.apply(shape)
    val pointer = context.putSynthesizedSchema(synthesizedName, schema)
    MediaTypeObject.builder.schema(Schema.builder.ref(pointer).build).build
  }

  private def createResponseDocumentIfNeeded(
      mediaType: String,
      context: Context[T],
      bindingIndex: HttpBindingIndex,
      responseBuilder: ResponseObject.Builder,
      operationOrError: Shape
  ): ResponseObject.Builder = {
    val bindings = bindingIndex.getResponseBindings(
      operationOrError,
      HttpBinding.Location.DOCUMENT
    )
    // If the operation doesn't have any document bindings, then do nothing.
    if (bindings.isEmpty) responseBuilder
    else {
      // Document bindings needs to be synthesized into a new schema that contains
      // just the document bindings separate from other parameters.
      val messageType =
        if (operationOrError.isInstanceOf[OperationShape])
          Smithy4sAbstractRestProtocol.MessageType.RESPONSE
        else Smithy4sAbstractRestProtocol.MessageType.ERROR
      // This "synthesizes" a new schema that just contains the document bindings.
      // While we *could* just use the referenced output/error shape as-is, that
      // would be a bad idea; traits applied to shapes in Smithy can contextually
      // influence what the resulting JSON schema or OpenAPI. Consider the
      // following examples:
      //
      // 1. If the same shape is reused as input and output, then some members
      //    might be bound to query string parameters, and query string params
      //    aren't relevant on output. Trying to use the same schema derived
      //    from the reused input/output shape would result in a broken API.
      // 2. What if the input/output shape doesn't bind anything to the query
      //    string, headers, path, etc? Couldn't it then be used as-is with
      //    the name given in the Smithy model? Yes, technically it could, but
      //    that's also a bad idea. If/when you want to add a header or query
      //    string parameter, then you now need to break your generated OpenAPI
      //    schema, particularly if the shapes was reused throughout your model
      //    outside of top-level inputs, outputs, and errors.
      // The safest thing to do here is to always synthesize a new schema that
      // just includes the document bindings.
      // **NOTE: this same blurb applies to why we do this on input.**
      val schema =
        createDocumentSchema(
          context,
          operationOrError,
          bindings.asScala.toList,
          messageType
        )
      val synthesizedName = operationOrError.getId.getName + "ResponseContent"
      val pointer = context.putSynthesizedSchema(synthesizedName, schema)

      val memberNames = bindings.asScala.toList.map(_.getMemberName)
      val maybeExamples = createExamples(operationOrError)(
        ExampleNode.forOutputMembers(_, memberNames)
      )
      val builder =
        MediaTypeObject.builder.schema(Schema.builder.ref(pointer).build)
      maybeExamples.foreach(builder.examples)
      responseBuilder.putContent(mediaType, builder.build)
    }
  }

  def determineContentType(bindings: Iterable[HttpBinding]) = {
    val locations = Set(Location.DOCUMENT, Location.PAYLOAD)
    bindings.collectFirst {
      case binding if locations(binding.getLocation()) =>
        getDocumentMediaType()
    }
  }

  private def createInputExamples(operation: Shape, memberName: String) =
    createExamples(operation)(ExampleNode.forInputMember(_, memberName))

  private def createOutputExamples(operation: Shape, memberName: String) =
    createExamples(operation)(ExampleNode.forOutputMember(_, memberName))

  private def createExamples(
      operation: Shape
  )(
      createNode: ExamplesTrait.Example => ExampleNode
  ): Option[util.Map[String, Node]] = {
    val maybeCheckedExamples: Option[List[ExamplesTrait.Example]] =
      operation.getTrait(classOf[ExamplesTrait]).asScala.map { exampleTrait =>
        exampleTrait.getExamples.asScala.toList
      }

    val maybeUncheckedExamples: Option[List[ExamplesTrait.Example]] =
      operation.getTrait(classOf[UncheckedExamplesTrait]).asScala.map {
        uncheckedExampleTrait =>
          uncheckedExampleTrait
            .getExamples()
            .asScala
            .map { unchecked =>
              val builder =
                ExamplesTrait.Example
                  .builder()
                  .title(unchecked.getTitle())
                  .input(unchecked.getInput())
                  .output(unchecked.getOutput())
              if (unchecked.getDocumentation().isPresent()) {
                builder.documentation(unchecked.getDocumentation().get())
              }
              builder.build()
            }
            .toList
      }

    (maybeCheckedExamples |+| maybeUncheckedExamples).map { examples =>
      examples
        .map(createNode(_).build)
        .collect { case Some(exampleNode) => exampleNode }
        .toMap
        .asJava
    }
  }
}
