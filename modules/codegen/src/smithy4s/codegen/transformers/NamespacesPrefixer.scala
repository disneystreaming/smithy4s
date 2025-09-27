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
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.node.StringNode
import software.amazon.smithy.model.shapes._
import software.amazon.smithy.model.traits.Trait

import java.util.stream.Collectors
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters.RichOptional
import cats.syntax.all._

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

      private def transformBaseShape[T <: Shape, B <: AbstractShapeBuilder[
        B,
        T
      ]](shape: T, builderField: T => B): T = {
        if (namespacesToTransform.exists(shape.getId.getNamespace.startsWith)) {
          builderField(shape).transformId
            .transformMixins(shape.getMixins())
            .transformTraits
            .build()
        } else {
          shape
        }
      }

      override def operationShape(shape: OperationShape): Shape = {
        shape
          .toBuilder()
          .transformId
          .input(
            shape.getInput.map[ShapeId](renameNamespaceForId).toScala.orNull
          )
          .output(
            shape.getOutput.map[ShapeId](renameNamespaceForId).toScala.orNull
          )
          .errors(shape.getErrors.asScala.map(renameNamespaceForId).asJava)
          .transformTraits
          .build()
      }

      override def stringShape(shape: StringShape): Shape =
        transformBaseShape(shape, (_: StringShape).toBuilder)

      override def bigDecimalShape(shape: BigDecimalShape): Shape =
        transformBaseShape(shape, (_: BigDecimalShape).toBuilder)

      override def structureShape(shape: StructureShape): Shape = {
        val withMembers = shape
          .toBuilder()
          .transformId
          .members {
            shape
              .getAllMembers()
              .asScala
              .map { case (_, memberShape) =>
                transformMemberShape(memberShape)
              }
              .toList
              .asJava
          }
        withMembers
          .transformMixins(shape.getMixins)
          .transformTraits
          .build()
      }

      implicit def toShapeOps[S <: Shape, B <: AbstractShapeBuilder[B, S]](
          builder: AbstractShapeBuilder[B, S]
      ): ShapeOps[S, B] =
        new ShapeOps[S, B](
          namespacesToTransform = namespacesToTransform,
          resolveShapeId = resolveShapeId,
          renameNamespaceForId = renameNamespaceForId,
          transformer = self
        )(builder = builder)

      override def resourceShape(shape: ResourceShape): Shape = {
        shape
          .toBuilder()
          .transformId
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
          .transformTraits
          .build()
      }

      override def booleanShape(shape: BooleanShape): Shape =
        transformBaseShape(shape, (_: BooleanShape).toBuilder)

      override def serviceShape(shape: ServiceShape): Shape = {
        shape
          .toBuilder()
          .transformId
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
          .transformTraits
          .build()
      }

      override def integerShape(shape: IntegerShape): Shape =
        transformBaseShape(shape, (_: IntegerShape).toBuilder)

      override def unionShape(shape: UnionShape): Shape =
        shape
          .toBuilder()
          .transformId
          .members {
            shape
              .getAllMembers()
              .asScala
              .map { case (_, memberShape) =>
                transformMemberShape(memberShape)
              }
              .toList
              .asJava
          }
          .transformTraits
          .build()

      override def longShape(shape: LongShape): Shape =
        transformBaseShape(shape, (_: LongShape).toBuilder)

      override def doubleShape(shape: DoubleShape): Shape =
        transformBaseShape(shape, (_: DoubleShape).toBuilder)

      override def bigIntegerShape(shape: BigIntegerShape): Shape =
        transformBaseShape(shape, (_: BigIntegerShape).toBuilder)

      override def shortShape(shape: ShortShape): Shape =
        transformBaseShape(shape, (_: ShortShape).toBuilder)

      override def mapShape(shape: MapShape): Shape = {
        shape
          .toBuilder()
          .transformId
          .key(transformMemberShape(shape.getKey()))
          .value(transformMemberShape(shape.getValue()))
          .transformTraits
          .build()
      }

      override def byteShape(shape: ByteShape): Shape =
        transformBaseShape(shape, (_: ByteShape).toBuilder)

      override def documentShape(shape: DocumentShape): Shape =
        transformBaseShape(shape, (_: DocumentShape).toBuilder)

      override def floatShape(shape: FloatShape): Shape =
        transformBaseShape(shape, (_: FloatShape).toBuilder)

      override def blobShape(shape: BlobShape): Shape =
        transformBaseShape(shape, (_: BlobShape).toBuilder)

      override def timestampShape(shape: TimestampShape): Shape =
        transformBaseShape(shape, (_: TimestampShape).toBuilder)

      override def memberShape(shape: MemberShape): Shape =
        transformMemberShape(shape)

      override def listShape(shape: ListShape): Shape =
        shape
          .toBuilder()
          .transformId
          .member(transformMemberShape(shape.getMember()))
          .transformTraits
          .build()

      private def transformMemberShape(shape: MemberShape): MemberShape = {
        shape
          .toBuilder()
          .transformId
          .target(renameNamespaceForId(shape.getTarget))
          .transformTraits
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
            arrayNode.getElements.asScala.toList
              .traverse {
                case stringNode: StringNode => Right(stringNode.getValue)
                case other =>
                  Left(
                    s"All elements in $TransformedNamespacesNamespacesToTransformMetadataKey must be strings, got: $other"
                  )
              }
              .map(_.toSet)
          }
      } yield Some(NamespacesPrefixerParams(prefix, namespacesToTransform))
    }

  }
}

private class ShapeOps[S <: Shape, B <: AbstractShapeBuilder[B, S]](
    namespacesToTransform: Set[String],
    resolveShapeId: ShapeId => Option[Shape],
    renameNamespaceForId: ShapeId => ShapeId,
    transformer: ShapeVisitor[Shape]
)(val builder: AbstractShapeBuilder[B, S]) {

  def transformId: B = builder.id(renameNamespaceForId(builder.getId))

  def transformTraits: B =
    builder.traits {
      builder
        .build()
        .getAllTraits()
        .asScala
        .values
        .map(transformTrait)
        .toList
        .asJava
    }

  def transformMixins(sourceMixins: java.util.Set[ShapeId]): B =
    builder
      .clearMixins()
      .mixins {
        sourceMixins.asScala
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
              sourceShape.accept(transformer)
            } else {
              sourceShape
            }
          }
          .toSet
          .asJava
      }

  private def transformTrait(_trait: Trait): Trait = {
    val shapeId = _trait.toShapeId
    if (namespacesToTransform.exists(shapeId.getNamespace.startsWith)) {
      val newShapeId = renameNamespaceForId(shapeId)
      val node = _trait.toNode()
      new Trait {
        override def toShapeId(): ShapeId = newShapeId
        override def toNode(): Node = node
      }
    } else {
      _trait
    }
  }
}
