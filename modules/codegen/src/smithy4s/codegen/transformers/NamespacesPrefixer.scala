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
import software.amazon.smithy.model.node.StringNode
import software.amazon.smithy.model.shapes._

import java.util.stream.Collectors
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters.RichOptional

final class NamespacesPrefixer extends ProjectionTransformer {

  override def transform(context: TransformContext): Model = {
    val model = context.getModel()

    import NamespacesPrefixer._

    constructParams(model) match {
      case Left(errorMessage) =>
        throw new IllegalArgumentException(errorMessage)
      case Right(None) =>
        model
      case Right(Some(params)) =>
        val prefix = params.prefix
        val namespacesToTransform = params.namespacesToPrefix
        val reshaper = shapeNamespaceRenamer(
          renameNamespace = ns => s"$prefix.$ns",
          resolveShapeId = shapeId => model.getShape(shapeId).toScala,
          namespacesToTransform = namespacesToTransform
        )
        model
          .shapes()
          .collect(Collectors.toList[Shape])
          .asScala
          .foldLeft(model.toBuilder()) { (builder, shape) =>
            if (
              namespacesToTransform.exists(
                shape.getId().getNamespace().startsWith
              )
            ) {
              builder
                .removeShape(shape.getId())
                .addShape(shape.accept(reshaper))
            } else {
              builder
            }
          }
          .build()
    }
  }

  override def getName(): String = "NamespacesPrefixer"

  private def shapeNamespaceRenamer(
      renameNamespace: String => String,
      resolveShapeId: ShapeId => Option[Shape],
      namespacesToTransform: Set[String]
  ) =
    new ShapeVisitor[Shape] { self =>
      private def renameNamespaceForId(shapeId: ShapeId): ShapeId = {
        val namespace = shapeId.getNamespace
        if (namespacesToTransform.exists(namespace.startsWith)) {
          ShapeId.fromParts(
            renameNamespace(namespace),
            shapeId.getName,
            shapeId.getMember().toScala.orNull
          )
        } else shapeId
      }

      private def renamespaceForShape[T <: Shape, B <: AbstractShapeBuilder[
        B,
        T
      ]](shape: T, builderField: T => B): T = {
        if (namespacesToTransform.exists(shape.getId.getNamespace.startsWith)) {
          builderField(shape).id(renameNamespaceForId(shape.getId)).build()
        } else {
          shape
        }
      }

      override def operationShape(shape: OperationShape): Shape = {
        shape
          .toBuilder()
          .id(renameNamespaceForId(shape.getId))
          .input(
            shape.getInput.map[ShapeId](renameNamespaceForId).toScala.orNull
          )
          .output(
            shape.getOutput.map[ShapeId](renameNamespaceForId).toScala.orNull
          )
          .errors(shape.getErrors.asScala.map(renameNamespaceForId).asJava)
          .build()
      }

      override def stringShape(shape: StringShape): Shape =
        renamespaceForShape(shape, (_: StringShape).toBuilder)

      override def bigDecimalShape(shape: BigDecimalShape): Shape =
        renamespaceForShape(shape, (_: BigDecimalShape).toBuilder)

      override def structureShape(shape: StructureShape): Shape = {
        shape
          .toBuilder()
          .id(renameNamespaceForId(shape.getId))
          .clearMembers()
          .members(
            shape
              .getAllMembers()
              .asScala
              .map { case (_, memberShape) =>
                transformMemberShape(memberShape)
              }
              .toList
              .asJava
          )
          .clearMixins()
          .mixins {
            shape
              .getMixins()
              .asScala
              .map { mixinShapeId =>
                val sourceShape = resolveShapeId(mixinShapeId).getOrElse(
                  throw new IllegalArgumentException(
                    s"Cannot resolve mixin shape $mixinShapeId while renaming namespaces"
                  )
                )
                if (
                  namespacesToTransform.exists(
                    mixinShapeId.getNamespace.startsWith
                  )
                ) {
                  sourceShape.accept(self)
                } else {
                  sourceShape
                }
              }
              .toSet
              .asJava
          }
          .build()
      }

      override def resourceShape(shape: ResourceShape): Shape = {
        shape
          .toBuilder()
          .id(renameNamespaceForId(shape.getId))
          .identifiers(
            shape.getIdentifiers.asScala
              .map { case (name, id) =>
                (name, renameNamespaceForId(id))
              }
              .toMap
              .asJava
          )
          .properties(
            shape.getProperties.asScala
              .map { case (name, id) =>
                (name, renameNamespaceForId(id))
              }
              .toMap
              .asJava
          )
          .put(shape.getPut.toScala.map[ShapeId](renameNamespaceForId).orNull)
          .create(
            shape.getCreate.map[ShapeId](renameNamespaceForId).toScala.orNull
          )
          .read(shape.getRead.map[ShapeId](renameNamespaceForId).toScala.orNull)
          .update(
            shape.getUpdate.map[ShapeId](renameNamespaceForId).toScala.orNull
          )
          .delete(
            shape.getDelete.map[ShapeId](renameNamespaceForId).toScala.orNull
          )
          .list(shape.getList.map[ShapeId](renameNamespaceForId).toScala.orNull)
          .collectionOperations(
            shape.getCollectionOperations.asScala
              .map(id => renameNamespaceForId(id))
              .toSet
              .asJava
          )
          .build()
      }

      override def booleanShape(shape: BooleanShape): Shape =
        renamespaceForShape(shape, (_: BooleanShape).toBuilder)

      override def serviceShape(shape: ServiceShape): Shape = {
        shape
          .toBuilder()
          .id(renameNamespaceForId(shape.getId))
          .operations(
            shape.getOperations.asScala
              .map(id => renameNamespaceForId(id))
              .toSet
              .asJava
          )
          .resources(
            shape.getResources.asScala
              .map(id => renameNamespaceForId(id))
              .toSet
              .asJava
          )
          .rename(
            shape.getRename.asScala
              .map { case (id, name) => (renameNamespaceForId(id), name) }
              .toMap
              .asJava
          )
          .errors(
            shape.getErrors.asScala
              .map(id => renameNamespaceForId(id))
              .toList
              .asJava
          )
          .build()
      }

      override def integerShape(shape: IntegerShape): Shape =
        renamespaceForShape(shape, (_: IntegerShape).toBuilder)

      override def unionShape(shape: UnionShape): Shape =
        shape
          .toBuilder()
          .id(renameNamespaceForId(shape.getId))
          .clearMembers()
          .members(
            shape
              .getAllMembers()
              .asScala
              .map { case (_, memberShape) =>
                transformMemberShape(memberShape)
              }
              .toList
              .asJava
          )
          .build()

      override def longShape(shape: LongShape): Shape =
        renamespaceForShape(shape, (_: LongShape).toBuilder)

      override def doubleShape(shape: DoubleShape): Shape =
        renamespaceForShape(shape, (_: DoubleShape).toBuilder)

      override def bigIntegerShape(shape: BigIntegerShape): Shape =
        renamespaceForShape(shape, (_: BigIntegerShape).toBuilder)

      override def shortShape(shape: ShortShape): Shape =
        renamespaceForShape(shape, (_: ShortShape).toBuilder)

      override def mapShape(shape: MapShape): Shape = {
        shape
          .toBuilder()
          .id(renameNamespaceForId(shape.getId))
          .key(transformMemberShape(shape.getKey()))
          .value(transformMemberShape(shape.getValue()))
          .build()
      }

      override def byteShape(shape: ByteShape): Shape =
        renamespaceForShape(shape, (_: ByteShape).toBuilder)

      override def documentShape(shape: DocumentShape): Shape =
        renamespaceForShape(shape, (_: DocumentShape).toBuilder)

      override def floatShape(shape: FloatShape): Shape =
        renamespaceForShape(shape, (_: FloatShape).toBuilder)

      override def blobShape(shape: BlobShape): Shape =
        renamespaceForShape(shape, (_: BlobShape).toBuilder)

      override def timestampShape(shape: TimestampShape): Shape =
        renamespaceForShape(shape, (_: TimestampShape).toBuilder)

      override def memberShape(shape: MemberShape): Shape =
        transformMemberShape(shape)

      override def listShape(shape: ListShape): Shape =
        shape
          .toBuilder()
          .id(renameNamespaceForId(shape.getId))
          .member(transformMemberShape(shape.getMember()))
          .build()

      private def transformMemberShape(shape: MemberShape): MemberShape = {
        shape
          .toBuilder()
          .id(renameNamespaceForId(shape.getId))
          .target(renameNamespaceForId(shape.getTarget))
          .build()
      }
    }

}

object NamespacesPrefixer {
  val name = "NamespacesPrefixer"

  val TransformedNamespacesPrefixMetadataKey =
    "smithy4sTransformedNamespacesPrefix"
  val TransformedNamespacesNamespacesToTransformMetadataKey =
    "smithy4sTransformedNamespacesNamespacesToTransform"

  private[transformers] case class NamespacesPrefixerParams(
      val prefix: String,
      val namespacesToPrefix: Set[String]
  )

  private[transformers] def constructParams(
      model: Model
  ): Either[String, Option[NamespacesPrefixerParams]] = {
    val prefixMetadata =
      model.getMetadata.get(TransformedNamespacesPrefixMetadataKey)
    val namespacesToTransformMetadata = model.getMetadata.get(
      TransformedNamespacesNamespacesToTransformMetadataKey
    )

    // Validation logic
    val hasPrefix = prefixMetadata != null
    val hasNamespaces = namespacesToTransformMetadata != null
    val namespacesCount = if (hasNamespaces) {
      namespacesToTransformMetadata
        .asArrayNode()
        .toScala
        .map(_.getElements.size())
        .getOrElse(0)
    } else 0

    if (namespacesCount > 0 && !hasPrefix) {
      Left(
        s"$TransformedNamespacesNamespacesToTransformMetadataKey is defined with length >= 1 but $TransformedNamespacesPrefixMetadataKey is not defined"
      )
    } else if (hasPrefix && namespacesCount == 0) {
      Left(
        s"$TransformedNamespacesPrefixMetadataKey is defined but $TransformedNamespacesNamespacesToTransformMetadataKey is not defined or has length 0"
      )
    } else if (!hasPrefix && !hasNamespaces) {
      Right(None)
    } else {
      for {
        prefix <- prefixMetadata
          .asStringNode()
          .toScala
          .map(_.getValue)
          .toRight(s"$TransformedNamespacesPrefixMetadataKey must be a string")
        namespacesToTransform <- namespacesToTransformMetadata
          .asArrayNode()
          .toScala
          .toRight(
            s"$TransformedNamespacesNamespacesToTransformMetadataKey must be an array"
          )
          .flatMap { arrayNode =>
            try {
              Right(arrayNode.getElements.asScala.map {
                case stringNode: StringNode => stringNode.getValue
                case other =>
                  throw new IllegalArgumentException(
                    s"All elements in $TransformedNamespacesNamespacesToTransformMetadataKey must be strings, got: $other"
                  )
              }.toSet)
            } catch {
              case e: IllegalArgumentException => Left(e.getMessage)
            }
          }
      } yield Some(NamespacesPrefixerParams(prefix, namespacesToTransform))
    }
  }
}
