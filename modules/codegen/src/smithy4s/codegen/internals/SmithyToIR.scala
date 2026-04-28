/*
 *  Copyright 2021-2026 Disney Streaming
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

package smithy4s.codegen.internals

import alloy.StructurePatternTrait
import cats.data.NonEmptyList
import cats.implicits._
import smithy4s.meta.AdtMemberTrait
import smithy4s.meta.AdtTrait
import smithy4s.meta.BincompatAddedTrait
import smithy4s.meta.BincompatFriendlyTrait
import smithy4s.meta.ErrorMessageTrait
import smithy4s.meta.GenerateOpticsTrait
import smithy4s.meta.GenerateServiceProductTrait
import smithy4s.meta.IndexedSeqTrait
import smithy4s.meta.NoStackTraceTrait
import smithy4s.meta.PackedInputsTrait
import smithy4s.meta.RefinementTrait
import smithy4s.meta.ScalaImportsTrait
import smithy4s.meta.TypeclassTrait
import smithy4s.meta.UnpackedOutputTrait
import smithy4s.meta.ValidateNewtypeTrait
import smithy4s.meta.VectorTrait
import software.amazon.smithy.aws.traits.ServiceTrait
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.knowledge.TopDownIndex
import software.amazon.smithy.model.node._
import software.amazon.smithy.model.selector.PathFinder
import software.amazon.smithy.model.shapes._
import software.amazon.smithy.model.traits.DefaultTrait
import software.amazon.smithy.model.traits.RequiredTrait
import software.amazon.smithy.model.traits._

import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import scala.annotation.nowarn
import scala.jdk.CollectionConverters._
import scala.util.Try

import Type.Alias

private[codegen] object SmithyToIR {

  def apply(
      model: Model,
      namespace: String
  ): CompilationUnit = {
    val smithyToIR = new SmithyToIR(model, namespace)
    PostProcessor(
      CompilationUnit(namespace, smithyToIR.allDecls, smithyToIR.rendererConfig)
    )
  }

  private[codegen] def prettifyName(
      maybeSdkId: Option[String],
      shapeName: String
  ): String = {
    maybeSdkId
      .map(_.replaceAll("\\s+", ""))
      .getOrElse(shapeName)
  }

}

private[codegen] class SmithyToIR(
    model: Model,
    namespace: String
) {

  private val finder = PathFinder.create(model)

  // Contains mixins of the given shape that have matching fields.
  private val mixinsOfCache = new ConcurrentHashMap[ShapeId, Set[ShapeId]]()

  val allShapes =
    model
      .shapes()
      .iterator()
      .asScala
      .toList

  val rendererConfig = Renderer.Config.load(model.getMetadata().asScala.toMap)

  private sealed trait DefaultRenderMode
  private object DefaultRenderMode {
    case object Full extends DefaultRenderMode
    case object OptionOnly extends DefaultRenderMode
    case object NoDefaults extends DefaultRenderMode

    def fromString(str: String): Option[DefaultRenderMode] = str match {
      case "FULL"        => Some(Full)
      case "OPTION_ONLY" => Some(OptionOnly)
      case "NONE"        => Some(NoDefaults)
      case _             => None
    }
  }

  private val defaultRenderMode =
    model
      .getMetadata()
      .asScala
      .get("smithy4sDefaultRenderMode")
      .flatMap(_.asStringNode().asScala)
      .flatMap(f => DefaultRenderMode.fromString(f.getValue))
      .getOrElse(DefaultRenderMode.Full)

  private val smithy4sDefaultDynamicHintNamespacePatterns
      : Set[NamespacePattern] = Set(
    NamespacePattern.fromString("smithy.api"),
    NamespacePattern.fromString("smithy.api.*"),
    NamespacePattern.fromString("alloy"),
    NamespacePattern.fromString("alloy.*")
  )

  private val smithy4sRenderDynamicHintNamespacePatterns
      : Set[NamespacePattern] =
    smithy4sDefaultDynamicHintNamespacePatterns ++
      model
        .getMetadata()
        .asScala
        .get("smithy4sRenderDynamicHintNamespacePatterns")
        .toSet
        .flatMap((n: Node) => n.asArrayNode().asScala)
        .flatMap(_.getElements().asScala)
        .flatMap(
          _.asStringNode().asScala.map(n =>
            NamespacePattern.fromString(n.getValue)
          )
        )

  private val smithy4sDefaultBinCompatHintNamespacePatterns
      : Set[NamespacePattern] = Set(
    NamespacePattern.fromString("smithy"),
    NamespacePattern.fromString("smithy.*"),
    NamespacePattern.fromString("alloy"),
    NamespacePattern.fromString("alloy.*")
  )

  // for now if you want to add more namespaces, you'll need to use a ModelTransformer
  // we don't allow metadata configuration since that would bypass model validation
  // such as not allowing bincompat trait in conjunction with ADT trait.
  private val smithy4sBinCompatHintNamespacePatterns: Set[NamespacePattern] =
    smithy4sDefaultBinCompatHintNamespacePatterns

  private val stdlibBincompatAddedShapes: Map[ShapeId, Hint.BincompatAdded] =
    model
      .getMetadata()
      .asScala
      .get("smithy4sBincompatPreludeAdditions")
      .toSet
      .flatMap((n: Node) => n.asArrayNode().asScala)
      .flatMap(_.getElements().asScala)
      .flatMap { (n: Node) =>
        n.asObjectNode().asScala.flatMap { obj =>
          for {
            shapeIdStr <- obj.getStringMember("shape").asScala
            versionStr <- obj.getStringMember("version").asScala
          } yield (
            ShapeId.from(shapeIdStr.getValue),
            Hint.BincompatAdded(VersionNumber.parse(versionStr.getValue))
          )
        }
      }
      .toMap

  private def fieldModifier(member: MemberShape): Field.Modifier = {
    val hasRequired = member.hasTrait(classOf[RequiredTrait])
    val hasNullable = member.hasTrait(classOf[alloy.NullableTrait])
    val defaultNode =
      member.getTrait(classOf[DefaultTrait]).asScala.map(_.toNode)
    val defaultTypedNode = defaultRenderMode match {
      case DefaultRenderMode.Full       => maybeDefault(member).map(_.typedNode)
      case DefaultRenderMode.OptionOnly => None
      case DefaultRenderMode.NoDefaults => None
    }
    Field.Modifier(
      hasRequired,
      hasNullable,
      defaultNode.map(Field.Default(_, defaultTypedNode))
    )
  }

  def allDecls = allShapes
    .filter(_.getId().getNamespace() == namespace)
    // Only structure mixins should be generated
    .filterNot { s => s.hasTrait(classOf[MixinTrait]) && !s.isStructureShape() }
    .flatMap(_.accept(toIRVisitor(renderAdtMemberStructures = false)))
    .toList

  def toIRVisitor(
      renderAdtMemberStructures: Boolean
  ): ShapeVisitor.Default[Option[Decl]] =
    new ShapeVisitor.Default[Option[Decl]] {

      override protected def getDefault(shape: Shape): Option[Decl] = {
        val hints = SmithyToIR.this.hints(shape)

        val recursive = hints.exists {
          case Hint.Trait => true
          case _          => false
        }

        shape.tpe.flatMap {
          case Type.Alias(_, name, tpe: Type.ExternalType, isUnwrapped) =>
            val newHints =
              hints.filterNot(_.sameNativeTrait(tpe.refinementHint))
            TypeAlias(
              shape.getId(),
              name,
              tpe,
              isUnwrapped,
              recursive,
              newHints
            ).some
          case Type.Alias(_, name, tpe, isUnwrapped) =>
            TypeAlias(
              shape.getId(),
              name,
              tpe,
              isUnwrapped,
              recursive,
              hints
            ).some
          case Type.ValidatedAlias(_, name, tpe) =>
            ValidatedTypeAlias(
              shape.getId(),
              name,
              tpe,
              recursive,
              hints
            ).some
          case Type.PrimitiveType(_) => None
          case other =>
            TypeAlias(
              shape.getId(),
              shape.name,
              other,
              isUnwrapped = false,
              recursive,
              hints
            ).some
        }
      }

      override def memberShape(x: MemberShape): Option[Decl] = None

      private def doFieldsMatch(
          mixinId: ShapeId,
          fields: List[Field]
      ): Boolean = {
        val mixin: StructureShape =
          model
            .getShape(mixinId)
            .asScala
            .flatMap(_.asStructureShape.asScala)
            .getOrElse(
              throw new IllegalArgumentException(
                s"Unable to find mixin with id: $mixinId"
              )
            )
        val mixinMembers = mixin.getAllMembers().asScala
        mixinMembers.forall { case (memberName, member) =>
          fields
            .find(_.name == memberName)
            .forall { field =>
              field.modifier.typeMod == fieldModifier(member).typeMod
            }
        }
      }

      override def structureShape(shape: StructureShape): Option[Decl] = {
        val hints = SmithyToIR.this.hints(shape)
        val isTrait = hints.exists {
          case Hint.Trait => true
          case _          => false
        }
        val rec = isRecursive(shape.getId()) || isTrait

        val fields = shape.fields
        val filteredMixins = getMixinsMatchingFields(shape)
        val mixins = filterMixinsExistOnParentAdt(filteredMixins.toSet, shape)
          .flatMap(_.tpe)
          .toList
        val isMixin = shape.hasTrait(classOf[MixinTrait])

        val p =
          Product(
            shapeId = shape.getId(),
            name = shape.name,
            fields = fields,
            mixins = mixins,
            recursive = rec,
            hints = hints,
            isMixin = isMixin
          ).some
        if (isPartOfAdt(shape)) {
          if (renderAdtMemberStructures) p else None
        } else p
      }

      private def getMixins(shape: UnionShape): List[Type] = {
        getMixinShapeIds(shape).flatMap(_.tpe)
      }

      private def getMixinShapeIds(shape: UnionShape): List[ShapeId] = {
        val memberTargets = shape
          .members()
          .asScala
          .toList
          .map(mem => model.expectShape(mem.getTarget))

        val mixins: List[Set[ShapeId]] = memberTargets
          .map(getMixinsMatchingFields)

        val result =
          if (mixins.isEmpty) Set.empty else mixins.reduce(_ intersect _)

        result.toList
      }

      private def getMixinsMatchingFields(shape: Shape): Set[ShapeId] = {
        def allMixinsOf(s: Shape): Set[ShapeId] =
          s.getMixins.asScala.toSet[ShapeId].flatMap { m =>
            allMixinsOf(model.expectShape(m)) + m
          }

        mixinsOfCache
          .computeIfAbsent(
            shape.getId,
            _ =>
              allMixinsOf(shape)
                // This filter is the more intensive part worth caching
                .filter(doFieldsMatch(_, shape.fields))
          )
      }

      // Filters out any mixins which exist on the parent ADT (if it is part of an ADT)
      // This is so the case classes in the ADT won't also extend the same mixins
      // as the parent sealed trait. This leads to cleaner generated code (no redundancy).
      private def filterMixinsExistOnParentAdt(
          mixinIds: Set[ShapeId],
          shape: StructureShape
      ): Set[ShapeId] = {
        getAdtParent(shape) match {
          case None => mixinIds
          case Some(parentId) =>
            model.expectShape(parentId).asUnionShape.asScala match {
              case None => mixinIds
              case Some(union) =>
                val unionMixins = getMixinShapeIds(union)
                mixinIds.filter(!unionMixins.contains(_))
            }
        }
      }

      override def unionShape(shape: UnionShape): Option[Decl] = {
        val rec = isRecursive(shape.getId())

        val mixins =
          if (shape.hasTrait(classOf[AdtTrait])) getMixins(shape)
          else List.empty

        val hints = SmithyToIR.this.hints(shape)
        val isTrait = hints.exists {
          case Hint.Trait => true
          case _          => false
        }
        NonEmptyList.fromList(shape.alts).map { case alts =>
          Union(shape.getId(), shape.name, alts, mixins, rec || isTrait, hints)
        }
      }

      override def stringShape(shape: StringShape): Option[Decl] =
        (shape match {
          case T.enumeration(e) => {
            val pseudoEnumShape =
              EnumShape.fromStringShape(shape, true).asScala match {
                case Some(shape) =>
                  shape
                    .toBuilder()
                    .asInstanceOf[EnumShape.Builder]
                    .build()
                case None => {
                  val namedEnumTrait = {
                    val defs = e.getValues().asScala.zipWithIndex.map {
                      case (enumDef, idx) =>
                        enumDef.getName().asScala match {
                          case Some(_) => enumDef
                          case None =>
                            enumDef
                              .toBuilder()
                              .name(
                                EnumUtil
                                  .enumValueClassName(
                                    None,
                                    enumDef.getValue,
                                    idx
                                  )
                              )
                              .build()
                        }
                    }
                    val builder = e.toBuilder().clearEnums()
                    defs.foreach(builder.addEnum)
                    builder.build()
                  }
                  EnumShape
                    .builder()
                    .id(shape.getId())
                    .source(shape.getSourceLocation())
                    .addTraits(
                      shape
                        .getAllTraits()
                        .values()
                        .asScala
                        .filterNot(
                          _.toShapeId() == ShapeId.from("smithy.api#enum")
                        )
                        .asJavaCollection
                    )
                    .asInstanceOf[EnumShape.Builder]
                    .setMembersFromEnumTrait(namedEnumTrait)
                    .build()
                }
              }
            enumShape(pseudoEnumShape)
          }
          case _ => this.getDefault(shape)
        })

      override def enumShape(shape: EnumShape): Option[Decl] = {
        val enumValues = shape.getEnumValues()
        val values = shape
          .members()
          .asScala
          .zipWithIndex
          .map { case (member, index) =>
            val name = member.getMemberName()
            val value = enumValues.get(name)
            EnumValue(
              value = value,
              intValue = index,
              name = name,
              realName = name,
              hints = hints(member)
            )
          }
          .toList

        val isOpen = shape.hasTrait(classOf[alloy.OpenEnumTrait])
        val openEnumHint = if (isOpen) List(Hint.OpenEnum) else List.empty

        Enumeration(
          shape.getId(),
          shape.name,
          if (isOpen) EnumTag.OpenStringEnum else EnumTag.StringEnum,
          values,
          hints = hints(shape) ++ openEnumHint
        ).some
      }

      override def intEnumShape(shape: IntEnumShape): Option[Decl] = {
        val enumValues = shape.getEnumValues()
        val values = shape
          .members()
          .asScala
          .map { member =>
            val name = member.getMemberName()
            val value = enumValues.get(name)
            EnumValue(
              value = name,
              intValue = value,
              name = name,
              realName = name,
              hints = hints(member)
            )
          }
          .toList

        val isOpen = shape.hasTrait(classOf[alloy.OpenEnumTrait])
        val openEnumHint = if (isOpen) List(Hint.OpenEnum) else List.empty

        Enumeration(
          shape.getId(),
          shape.name,
          if (isOpen) EnumTag.OpenIntEnum else EnumTag.IntEnum,
          values,
          hints(shape) ++ openEnumHint
        ).some
      }

      override def serviceShape(shape: ServiceShape): Option[Decl] = {
        val generalErrors: List[Type] =
          shape
            .getErrorsSet()
            .asScala
            .toList
            .sortBy(_.toShapeId)
            .map(_.tpe)
            .collect { case Some(tpe) => tpe }

        // Aggregates both the operations of the current entity and the ones
        // in the sub-entities.
        def recursiveOperations(
            service: ServiceShape
        ): List[ShapeId] =
          TopDownIndex
            .of(model)
            .getContainedOperations(service)
            .asScala
            .map(_.getId())
            .toList

        val operations = recursiveOperations(shape)
          .map(model.getShape(_).asScala)
          .collect { case Some(S.Operation(op)) =>
            val inputType =
              op.getInputShape().tpe.getOrElse(Type.unit)

            val params =
              op.getInputShape()
                .shape
                .toList
                .flatMap(_.fields)

            def streamedMember(shapeId: ShapeId) =
              shapeId.shape
                .map(_.members().asScala.toList)
                .flatMap(_.collectFirstSome(streamingField))
            val streamedInput = streamedMember(op.getInputShape())
            val streamedOutput = streamedMember(op.getOutputShape())

            val errorTypes = {
              generalErrors ++ op
                .getErrorsSet()
                .asScala
                .toList
                .sortBy(_.toShapeId)
                .flatMap(_.tpe)
            }.distinct

            val outputType =
              op.getOutputShape().tpe.getOrElse(Type.unit)

            val unpackedOutputHint: Option[Hint] =
              if (op.hasTrait(classOf[UnpackedOutputTrait])) {
                op.getOutputShape()
                  .shape
                  .toList
                  .flatMap(_.fields)
                  .headOption
                  .map { field =>
                    Hint.UnpackedOutput(field.name, field.tpe, field.modifier)
                  }
              } else None

            Operation(
              op.getId(),
              op.name,
              uncapitalise(op.name),
              params,
              inputType,
              errorTypes,
              outputType,
              streamedInput,
              streamedOutput,
              hints(op) ++ unpackedOutputHint.toList
            )
          }

        val serviceHints = hints(shape)
        val maybeSdkId =
          shape
            .getTrait(classOf[ServiceTrait])
            .asScala
            .flatMap(st => Option(st.getSdkId()))
            .filterNot(_.isEmpty)

        val prettyName = SmithyToIR.prettifyName(maybeSdkId, shape.name)

        Service(
          shape.getId(),
          prettyName,
          operations,
          serviceHints,
          shape.getVersion()
        ).some
      }
    }

  private def isRecursive(id: ShapeId): Boolean = {
    // A shape is recursive if there is a relationship from itself to itself.
    val shape = model.expectShape(id)
    val paths = finder.search(shape, List(shape).asJava)
    !paths.isEmpty()
  }

  private val toType: ShapeVisitor[Option[Type]] =
    new ShapeVisitor[Option[Type]] {
      // See https://awslabs.github.io/smithy/1.0/spec/core/prelude-model.html?highlight=primitiveboolean#prelude-shapes
      val primitiveAliases = List(
        "PrimitiveBoolean",
        "PrimitiveByte",
        "PrimitiveInteger",
        "PrimitiveLong",
        "PrimitiveFloat",
        "PrimitiveDouble"
      )
      val smithyNamespace = "smithy.api"

      private def isUnboxedPrimitive(shapeId: ShapeId): Boolean =
        shapeId.getNamespace() == smithyNamespace && primitiveAliases.contains(
          shapeId.getName()
        )

      private sealed trait ExternalTypeInfo
      private object ExternalTypeInfo {
        case class RefinementInfo(trt: RefinementTrait) extends ExternalTypeInfo
        case class StructurePatternInfo(trt: StructurePatternTrait)
            extends ExternalTypeInfo
      }

      private def getExternalTypeInfo(
          shape: Shape
      ): Option[(Trait, ExternalTypeInfo)] = {
        shape
          .getAllTraits()
          .asScala
          .flatMap { case (_, trt) =>
            val refinement = model
              .getShape(trt.toShapeId)
              .asScala
              .flatMap(_.getTrait(classOf[RefinementTrait]).asScala)
              .map(rt => trt -> ExternalTypeInfo.RefinementInfo(rt))
            def idRef =
              if (trt.toShapeId == IdRefTrait.ID) {
                val rt = RefinementTrait
                  .builder()
                  .targetType("smithy4s.ShapeId")
                  .build()
                Some(trt -> ExternalTypeInfo.RefinementInfo(rt))
              } else None
            refinement.orElse(idRef)
          }
          .headOption // Shapes can have at most ONE trait that has the refined trait
          .orElse {
            shape.getTrait(classOf[StructurePatternTrait]).asScala.map { trt =>
              trt -> ExternalTypeInfo.StructurePatternInfo(trt)
            }
          }
      }

      private def getExternalOrBase(shape: Shape, base: Type): Type =
        getExternalTypeInfo(shape)
          .map {
            case (trt, ExternalTypeInfo.RefinementInfo(refined)) =>
              val baseTypeParams = base match {
                case c: Type.Collection => List(c.member)
                case m: Type.Map        => List(m.key, m.value)
                case other              => List(other)
              }
              Type.ExternalType(
                shape.name,
                refined.getTargetType(),
                if (refined.getParameterised()) baseTypeParams else List.empty,
                refined.getProviderImport().asScala,
                base,
                unfoldTrait(trt)
              )
            case (trt, ExternalTypeInfo.StructurePatternInfo(pattern)) =>
              Type.ExternalType(
                shape.name,
                s"${pattern.getTarget.namespace}.${pattern.getTarget.name}",
                List.empty,
                Some("smithy4s.internals.StructurePatternRefinementProvider._"),
                base,
                unfoldTrait(trt)
              )
          }
          .getOrElse(base)

      private def isExternal(tpe: Type): Boolean = tpe match {
        case _: Type.ExternalType => true
        case _                    => false
      }

      private def isUnwrappedShape(shape: Shape): Boolean = {
        shape.hasTrait(classOf[smithy4s.meta.UnwrapTrait])
      }

      def primitive(
          shape: Shape,
          primitiveId: String,
          primitive: Primitive
      ): Option[Type] = {
        val externalOrBase =
          getExternalOrBase(shape, Type.PrimitiveType(primitive))
        if (
          shape.getId() != ShapeId.from(primitiveId) &&
          !isUnboxedPrimitive(shape.getId())
        ) {
          val shouldValidate =
            shape.hasTrait(classOf[ValidateNewtypeTrait])
          if (shouldValidate) {
            Type
              .ValidatedAlias(
                shape.getId().getNamespace(),
                shape.getId().getName(),
                externalOrBase
              )
              .some
          } else {
            Type
              .Alias(
                shape.getId().getNamespace(),
                shape.getId().getName(),
                externalOrBase,
                isUnwrappedShape(shape)
              )
              .some
          }
        } else externalOrBase.some
      }

      def blobShape(x: BlobShape): Option[Type] =
        if (x.getTrait(classOf[StreamingTrait]).isPresent()) {
          Type
            .Alias(
              x.getId().getNamespace(),
              x.getId().getName,
              Type.PrimitiveType(Primitive.Byte),
              isUnwrappedShape(x)
            )
            .some
        } else {
          primitive(x, "smithy.api#Blob", Primitive.Blob)
        }

      def booleanShape(x: BooleanShape): Option[Type] =
        primitive(x, "smithy.api#Boolean", Primitive.Bool)

      def getHints(tpe: Type, shape: Shape): List[Hint] = {
        val h = hints(shape)
        tpe match {
          case e: Type.ExternalType =>
            h.filterNot(_.sameNativeTrait(e.refinementHint))
          case _ => h
        }
      }

      def listShape(x: ListShape): Option[Type] = {
        x.getMember()
          .accept(this)
          .map { tpe =>
            if (x.hasTrait(classOf[SparseTrait])) {
              Type.Nullable(tpe)
            } else tpe
          }
          .map { tpe =>
            val _hints = hints(x)
            val memberHints = getHints(tpe, x.getMember)
            if (_hints.contains(Hint.UniqueItems)) {
              Type.Collection(CollectionType.Set, tpe, memberHints)
            } else if (_hints.contains(Hint.SpecializedList.Vector)) {
              Type.Collection(CollectionType.Vector, tpe, memberHints)
            } else if (_hints.contains(Hint.SpecializedList.IndexedSeq)) {
              Type.Collection(CollectionType.IndexedSeq, tpe, memberHints)
            } else {
              Type.Collection(CollectionType.List, tpe, memberHints)
            }
          }
          .map { tpe =>
            val externalOrBase =
              getExternalOrBase(x, tpe)
            val isUnwrapped = !isExternal(externalOrBase) || isUnwrappedShape(x)
            Type.Alias(x.namespace, x.name, externalOrBase, isUnwrapped)
          }
      }

      @nowarn("msg=class SetShape in package shapes is deprecated")
      override def setShape(x: SetShape): Option[Type] = {
        x.getMember()
          .accept(this)
          .map(mem =>
            Type
              .Collection(CollectionType.Set, mem, getHints(mem, x.getMember()))
          )
          .map { tpe =>
            val externalOrBase =
              getExternalOrBase(x, tpe)
            val isUnwrapped = !isExternal(externalOrBase) || isUnwrappedShape(x)
            Type.Alias(
              x.namespace,
              x.name,
              externalOrBase,
              isUnwrapped
            )
          }
      }

      def mapShape(x: MapShape): Option[Type] = (for {
        k <- x.getKey().accept(this)
        v <- x.getValue().accept(this).map { tpe =>
          if (x.hasTrait(classOf[SparseTrait])) Type.Nullable(tpe) else tpe
        }
        mapType =
          if (x.hasTrait(classOf[alloy.PreserveKeyOrderTrait]))
            MapType.SeqMap
          else MapType.Map
      } yield Type.Map(
        mapType,
        k,
        getHints(k, x.getKey()),
        v,
        getHints(v, x.getValue())
      )).map { tpe =>
        val externalOrBase =
          getExternalOrBase(x, tpe)
        val isUnwrapped = !isExternal(externalOrBase) || isUnwrappedShape(x)
        Type.Alias(x.namespace, x.name, externalOrBase, isUnwrapped)
      }

      def byteShape(x: ByteShape): Option[Type] =
        primitive(x, "smithy.api#Byte", Primitive.Byte)

      def shortShape(x: ShortShape): Option[Type] =
        primitive(x, "smithy.api#Short", Primitive.Short)

      def integerShape(x: IntegerShape): Option[Type] =
        primitive(x, "smithy.api#Integer", Primitive.Int)

      def longShape(x: LongShape): Option[Type] =
        primitive(x, "smithy.api#Long", Primitive.Long)

      def floatShape(x: FloatShape): Option[Type] = {
        primitive(x, "smithy.api#Float", Primitive.Float)
      }

      def documentShape(x: DocumentShape): Option[Type] =
        primitive(x, "smithy.api#Document", Primitive.Document)

      def doubleShape(x: DoubleShape): Option[Type] =
        primitive(x, "smithy.api#Double", Primitive.Double)

      def bigIntegerShape(x: BigIntegerShape): Option[Type] =
        primitive(x, "smithy.api#BigInteger", Primitive.BigInteger)

      def bigDecimalShape(x: BigDecimalShape): Option[Type] = x match {
        case shape if shape.getId() == durationShapeId =>
          Type.PrimitiveType(Primitive.Duration).some
        case T.durationSecondsFormat(_) =>
          if (x.getId() == ShapeId.from("smithy.api#BigDecimal"))
            Type.PrimitiveType(Primitive.Duration).some
          else
            Type
              .Alias(
                x.namespace,
                x.name,
                Type.PrimitiveType(Primitive.Duration),
                isUnwrapped = false
              )
              .some
        case _ =>
          primitive(x, "smithy.api#BigDecimal", Primitive.BigDecimal)
      }

      def operationShape(x: OperationShape): Option[Type] = None

      def resourceShape(x: ResourceShape): Option[Type] = None

      def serviceShape(x: ServiceShape): Option[Type] = None

      override def enumShape(x: EnumShape): Option[Type] =
        Type.Ref(x.namespace, x.name).some

      override def intEnumShape(x: IntEnumShape): Option[Type] =
        Type.Ref(x.namespace, x.name).some

      def stringShape(x: StringShape): Option[Type] = x match {
        case T.enumeration(_) => Type.Ref(x.namespace, x.name).some
        case shape if shape.getId() == uuidShapeId =>
          Type.PrimitiveType(Primitive.Uuid).some
        case shape if shape.getId() == localDateShapeId =>
          Type.PrimitiveType(Primitive.LocalDate).some
        case shape if shape.getId() == localTimeShapeId =>
          Type.PrimitiveType(Primitive.LocalTime).some
        case T.uuidFormat(_) =>
          if (x.getId() == ShapeId.from("smithy.api#String"))
            Type.PrimitiveType(Primitive.Uuid).some
          else
            Type
              .Alias(
                x.namespace,
                x.name,
                Type.PrimitiveType(Primitive.Uuid),
                isUnwrapped = false
              )
              .some
        case T.localDateFormat(_) =>
          if (x.getId() == ShapeId.from("smithy.api#String"))
            Type.PrimitiveType(Primitive.LocalDate).some
          else
            Type
              .Alias(
                x.namespace,
                x.name,
                Type.PrimitiveType(Primitive.LocalDate),
                isUnwrapped = false
              )
              .some
        case T.localTimeFormat(_) =>
          if (x.getId() == ShapeId.from("smithy.api#String"))
            Type.PrimitiveType(Primitive.LocalTime).some
          else
            Type
              .Alias(
                x.namespace,
                x.name,
                Type.PrimitiveType(Primitive.LocalTime),
                isUnwrapped = false
              )
              .some
        case _ =>
          primitive(x, "smithy.api#String", Primitive.String)
      }

      def structureShape(x: StructureShape): Option[Type] =
        if (x.getId() == ShapeId.fromParts("smithy.api", "Unit"))
          Some(Type.unit)
        else Type.Ref(x.namespace, x.name).some

      def unionShape(x: UnionShape): Option[Type] =
        Type.Ref(x.namespace, x.name).some

      @SuppressWarnings(Array("all"))
      def memberShape(x: MemberShape): Option[Type] =
        model.getShape(x.getTarget()).asScala.flatMap { shape =>
          val builder = (Shape.shapeToBuilder(shape: Shape): Any)
            .asInstanceOf[AbstractShapeBuilder[?, ?]]

          builder
            .addTraits(x.getAllTraits().asScala.map(_._2).asJavaCollection)

          builder
            .build()
            .asInstanceOf[Shape]
            .accept(this)
        }

      def timestampShape(x: TimestampShape): Option[Type] = x match {
        case shape if shape.getId() == offsetDateTimeShapeId =>
          Type.PrimitiveType(Primitive.OffsetDateTime).some
        case T.offsetDateTimeFormat(_) =>
          if (x.getId() == ShapeId.from("smithy.api#Timestamp"))
            Type.PrimitiveType(Primitive.OffsetDateTime).some
          else
            Type
              .Alias(
                x.namespace,
                x.name,
                Type.PrimitiveType(Primitive.OffsetDateTime),
                isUnwrapped = false
              )
              .some
        case _ => primitive(x, "smithy.api#Timestamp", Primitive.Timestamp)
      }

    }

  def toTypeRef(id: ToShapeId): Type.Ref = {
    val shapeId = id.toShapeId()
    Type.Ref(shapeId.getNamespace(), shapeId.getName())
  }

  object ConstraintTrait {
    def unapply(tr: Trait): Option[Trait] =
      tr match {
        case t: RangeTrait   => Some(t)
        case t: LengthTrait  => Some(t)
        case t: PatternTrait => Some(t)
        case _               => None
      }
  }

  // Captures the data representing the default value of a member shape.
  private def maybeDefault(shape: MemberShape): Option[Hint.Default] = {
    val maybeTrait = shape.getTrait(classOf[DefaultTrait])
    if (maybeTrait.isPresent()) {
      val tr = maybeTrait.get()
      // We're short-circuiting when encountering any external type,
      // as we do not have the means to instantiate them in a safe manner.
      def unfoldNodeAndTypeIfNotExternal(nodeAndType: NodeAndType) = {
        nodeAndType.tpe match {
          case _: Type.ExternalType => None
          case _                    => Some(unfoldNodeAndType(nodeAndType))
        }
      }
      val node = tr.toNode()
      val targetTpe = shape.tpe.get
      // Constructing the initial value for the refold
      val nodeAndType = targetTpe match {
        case Alias(_, _, tpe, true) => NodeAndType(node, tpe)
        case _                      => NodeAndType(node, targetTpe)
      }
      val maybeTree =
        recursion.anaM(unfoldNodeAndTypeIfNotExternal)(nodeAndType)
      maybeTree.map(Hint.Default(_))
    } else {
      None
    }
  }

  def maybeTypeclassesHint(shape: Shape): List[Hint.Typeclass] = {
    shape
      .getAllTraits()
      .asScala
      .flatMap { case (_, trt) =>
        model
          .getShape(trt.toShapeId)
          .asScala
          .flatMap(_.getTrait(classOf[TypeclassTrait]).asScala)
          .map(trt -> _)
      }
      .map { case (typeclassName, typeclassInfo) =>
        Hint.Typeclass(
          typeclassName.toShapeId,
          typeclassInfo.getTargetType,
          typeclassInfo.getInterpreter
        )
      }
      .toList
  }

  @annotation.nowarn(
    "msg=class UniqueItemsTrait in package traits is deprecated"
  )
  private def traitToHint(shape: Shape): PartialFunction[Trait, Hint] = {
    case _: ErrorTrait => Hint.Error
    case t: ProtocolDefinitionTrait =>
      val shapeIds = t.getTraits()
      val refs = shapeIds.asScala.map(shapeId =>
        Type.Ref(shapeId.getNamespace(), shapeId.getName())
      )
      Hint.Protocol(refs.toList)
    case _: PackedInputsTrait =>
      Hint.PackedInputs
    case d: DeprecatedTrait =>
      Hint.Deprecated(d.getMessage.asScala, d.getSince.asScala)
    case _: ErrorMessageTrait =>
      Hint.ErrorMessage
    case _: NoStackTraceTrait =>
      Hint.NoStackTrace
    case _: VectorTrait =>
      Hint.SpecializedList.Vector
    case _: IndexedSeqTrait =>
      Hint.SpecializedList.IndexedSeq
    case _: UniqueItemsTrait =>
      Hint.UniqueItems
    case _: GenerateServiceProductTrait =>
      Hint.GenerateServiceProduct
    case _: GenerateOpticsTrait =>
      Hint.GenerateOptics
    case s: ScalaImportsTrait =>
      Hint.ScalaImports(s.getValues().asScala.toList)
    case _: ValidateNewtypeTrait =>
      Hint.ValidateNewtype
    case _: TraitDefinition =>
      Hint.Trait
    case ConstraintTrait(tr) => Hint.Constraint(toTypeRef(tr), unfoldTrait(tr))
    case _: BincompatFriendlyTrait =>
      Hint.BincompatFriendly
    case b: BincompatAddedTrait =>
      Hint.BincompatAdded(VersionNumber.parse(b.getVersion()))

  }

  private def streamingOperation(
      op: OperationShape
  ): (Option[Shape], Option[Shape]) = {
    def forTarget(id: ShapeId): Option[MemberShape] = {
      model.getShape(id).asScala.flatMap { shape =>
        shape.members().asScala.find(isStreaming)
      }
    }
    (
      op.getInput().asScala.flatMap(forTarget),
      op.getOutput().asScala.flatMap(forTarget)
    )
  }

  private def documentationHint(shape: Shape): Option[Hint] = {
    def split(s: String) =
      s.replace("*/", "\\*\\/").linesIterator.toList
    val shapeDocs = shape
      .getTrait(classOf[DocumentationTrait])
      .asScala
      .foldMap(doc => split(doc.getValue()))

    val operationDocs: List[String] = {
      shape
        .asOperationShape()
        .asScala
        .toList
        .flatMap { op =>
          streamingOperation(op) match {
            case (Some(in), Some(out)) =>
              val maybeDoc = for {
                inMem <- in
                  .asMemberShape()
                  .asScala
                  .map(_.getMemberName)
                outMem <- out
                  .asMemberShape()
                  .asScala
                  .map(_.getMemberName)
              } yield s"This operation uses @streaming on both the input (${inMem}) and the output (${outMem})"
              maybeDoc.toList
            case (Some(in), None) =>
              val maybeDoc = for {
                inMem <- in
                  .asMemberShape()
                  .asScala
                  .map(_.getMemberName)
              } yield s"This operation uses @streaming on the input (${inMem})."
              maybeDoc.toList
            case (None, Some(out)) =>
              val maybeDoc = for {
                outMem <- out
                  .asMemberShape()
                  .asScala
                  .map(_.getMemberName)
              } yield s"This operation uses @streaming on the output (${outMem})."
              maybeDoc.toList
            case (None, None) =>
              List.empty
          }
        }
    }

    val httpDocs = shape
      .getTrait(classOf[HttpTrait])
      .asScala
      .map { http =>
        List(s"HTTP ${http.getMethod} ${http.getUri.toString}")
      }
      .getOrElse(List.empty)

    def getMemberDocs(shape: Shape): Map[String, List[String]] =
      shape match {
        case _: UnionShape => Map.empty
        case op: OperationShape =>
          op.getInput()
            .asScala
            .map(id => getMemberDocs(model.expectShape(id)))
            .getOrElse(Map.empty)
        case _ =>
          shape
            .members()
            .asScala
            .filterNot(isStreaming)
            .map { member =>
              val memberDocs =
                member.getTrait(classOf[DocumentationTrait]).asScala
              def targetDocs = model
                .expectShape(member.getTarget)
                .getTrait(classOf[DocumentationTrait])
                .asScala

              (
                member.getMemberName(),
                memberDocs.orElse(targetDocs)
              )
            }
            .collect { case (name, Some(v)) => (name, split(v.getValue())) }
            .toMap

      }

    val memberDocs = getMemberDocs(shape)
    val protocolSpecific = List(httpDocs).filter(_.nonEmpty)
    if (
      shapeDocs.nonEmpty || operationDocs.nonEmpty || memberDocs.nonEmpty || protocolSpecific.nonEmpty
    ) {
      Some(
        Hint.Documentation(
          shapeDocs ++ operationDocs,
          memberDocs,
          protocolSpecific
        )
      )
    } else None
  }

  private def hints(shape: Shape): List[Hint] = {
    val allTraits = shape.getAllTraits().asScala.values.toList
    val nonMetaTraits =
      allTraits
        .filterNot(_.toShapeId().getNamespace() == "smithy4s.meta")
        // traits from the synthetic namespace, e.g. smithy.synthetic.enum
        // don't have shapes in the model - so we can't generate hints for them.
        .filterNot(_.toShapeId().getNamespace() == "smithy.synthetic")
        // smithy.rules traits contain massive endpoint routing JSON and aren't used at runtime
        .filterNot(_.toShapeId().getNamespace() == "smithy.rules")
        // enumValue can be derived from enum schemas anyway, so we're removing it from hints
        .filterNot(_.toShapeId() == EnumValueTrait.ID)
        // remove box trait
        .filterNot(_.toShapeId() == BoxTrait.ID): @nowarn(
        "msg=class BoxTrait in package traits is deprecated"
      )

    val nonConstraintNonMetaTraits = nonMetaTraits.collect {
      case t if ConstraintTrait.unapply(t).isEmpty => t
    }

    val stdlibBincompatFriendlyTrait = {
      if (
        smithy4sBinCompatHintNamespacePatterns.exists(
          _.matches(shape.namespace)
        )
      ) {
        Some(Hint.BincompatFriendly)
      } else None
    }

    val stdlibBincompatAddedHint = stdlibBincompatAddedShapes.get(
      shape.getId()
    )

    allTraits.collect(traitToHint(shape)) ++
      stdlibBincompatFriendlyTrait ++
      stdlibBincompatAddedHint ++
      documentationHint(shape) ++
      nonConstraintNonMetaTraits
        .filter(tr =>
          tr.toShapeId != RequiredTrait.ID && tr.toShapeId != alloy.NullableTrait.ID
        )
        .map(unfoldTraitNonConstraint) ++
      maybeTypeclassesHint(shape)
  }

  case class AltInfo(name: String, tpe: Type, isAdtMember: Boolean) {
    def isUnit: Boolean = tpe == Type.unit
  }

  implicit class ShapeExt(shape: Shape) {
    def name = shape.getId().getName()

    def namespace = shape.getId().getNamespace()

    def tpe: Option[Type] = shape.accept(toType)

    def fields: List[Field] = {
      val noDefault =
        if (defaultRenderMode == DefaultRenderMode.NoDefaults)
          List(Hint.NoDefault)
        else List.empty
      val result = shape
        .members()
        .asScala
        .filterNot(isStreaming)
        .map { member =>
          val default =
            if (defaultRenderMode == DefaultRenderMode.Full)
              maybeDefault(member).toList
            else List.empty
          val modifier = fieldModifier(member)
          (
            member.getMemberName(),
            member.tpe,
            modifier,
            hints(member) ++ default ++ noDefault
          )
        }
        .zipWithIndex
        .collect {
          case ((name, Some(tpe: Type.ExternalType), modifier, hints), index) =>
            val newHints =
              hints.filterNot(_.sameNativeTrait(tpe.refinementHint))
            Field(name, tpe, modifier, index, newHints)
          case ((name, Some(tpe), modifier, hints), index) =>
            Field(name, tpe, modifier, index, hints)
        }
        .toList

      defaultRenderMode match {
        case DefaultRenderMode.Full =>
          implicit val modifierOrder = Field.Modifier.fullOrder
          result.sortBy(_.modifier)
        case DefaultRenderMode.OptionOnly =>
          implicit val modifierOrder = Field.Modifier.optionOnlyOrder
          result.sortBy(_.modifier)
        case DefaultRenderMode.NoDefaults => result
      }
    }

    def alts = {
      shape
        .members()
        .asScala
        .map { member =>
          val memberTarget =
            model.expectShape(member.getTarget)
          if (isPartOfAdt(memberTarget)) {
            val s = memberTarget
              .accept(toIRVisitor(renderAdtMemberStructures = true))
              .map(Left(_))
            (member.getMemberName(), s, hints(member))
          } else {
            (member.getMemberName(), member.tpe.map(Right(_)), hints(member))
          }
        }
        .collect {
          case (name, Some(Right(Type.unit)), h) =>
            Alt(name, UnionMember.UnitCase, h)
          case (name, Some(Right(tpe: Type.ExternalType)), h) =>
            Alt(
              name,
              UnionMember.TypeCase(tpe),
              h.filterNot(_.sameNativeTrait(tpe.refinementHint))
            )
          case (name, Some(Right(tpe)), h) =>
            Alt(name, UnionMember.TypeCase(tpe), h)
          case (name, Some(Left(p: Product)), h) =>
            Alt(name, UnionMember.ProductCase(p), h)
        }
        .toList
    }

    def getAltTypes: List[AltInfo] = {
      shape
        .members()
        .asScala
        .flatMap { member =>
          member.tpe.map { tpe =>
            val memberTarget = model.expectShape(member.getTarget)

            AltInfo(
              member.getMemberName(),
              tpe,
              isAdtMember = isPartOfAdt(memberTarget)
            )
          }
        }
        .toList
    }

  }

  private def isStreaming(member: MemberShape): Boolean =
    member
      .getTrait(classOf[StreamingTrait])
      .asScala
      .orElse(
        member
          .getTarget()
          .shape
          .flatMap(_.getTrait(classOf[StreamingTrait]).asScala)
      )
      .isDefined

  private def streamingField(member: MemberShape): Option[StreamingField] = {
    if (isStreaming(member)) {
      member.tpe.map { tpe =>
        StreamingField(member.getId().name, tpe, hints(member))
      }
    } else None
  }

  implicit class ShapeIdExt(shapeId: ShapeId) {
    def name = shapeId.getName()

    def namespace = shapeId.getNamespace()

    def shape: Option[Shape] = model.getShape(shapeId).asScala

    def tpe: Option[Type] =
      model.getShape(shapeId).asScala.flatMap(_.accept(toType))
  }

  private case class NodeAndType(node: Node, tpe: Type)

  private def isPartOfAdt(shape: Shape): Boolean = {
    shape.hasTrait(classOf[AdtMemberTrait]) ||
    getAdtParent(shape).isDefined
  }

  private def getAdtParent(shape: Shape): Option[ShapeId] = {
    val result = model
      .getMemberShapes()
      .asScala
      .toList
      .filter(_.getTarget == shape.toShapeId)
      .find(mem =>
        model.expectShape(mem.getContainer).hasTrait(classOf[AdtTrait])
      )

    result.map(_.getContainer)
  }

  private object UnRef {
    def unapply(tpe: Type): Option[Shape] = tpe match {
      case Type.Ref(ns, name) =>
        val maybeShape = model
          .getShape(ShapeId.fromParts(ns, name))
          .asScala
        maybeShape.map { shape =>
          val fromAdtMember = shape
            .getTrait(classOf[AdtMemberTrait])
            .asScala
            .map(_.getValue)
          val adtParent: Option[ShapeId] =
            fromAdtMember orElse getAdtParent(shape)
          adtParent match {
            case Some(parent) =>
              val cId = shape.getId
              val newNs =
                cId.getNamespace + "." + parent.getName.capitalize
              val error = new Exception(
                s"Shapes annotated with the adtMemberTrait must be structures. $cId is not a structure."
              )
              shape.asStructureShape.asScala
                // This error should never be thrown due to selector on AdtMemberTrait, but is here in case
                .getOrElse(throw error)
                .toBuilder
                .id(ShapeId.fromParts(newNs, cId.getName))
                .build()
            case _ => shape
          }
        }
      case _ => None
    }
  }

  case class UnhandledTraitBinding(node: Node, tpe: Type) extends Throwable {
    override def getMessage(): String =
      s"Unhandled trait binding:\ntype: $tpe\nvalue: ${Node.printJson(node)}"
  }

  private def unfoldNode(node: Node, shapeId: ShapeId): Fix[TypedNode] = {
    val nodeAndType = NodeAndType(node, shapeId.tpe.getOrElse(Type.unit))
    recursion.ana(unfoldNodeAndType)(nodeAndType)
  }

  private def unfoldTrait(tr: Trait): Hint.Native = {
    Hint.Native(
      tr.toShapeId,
      cats.Eval.later(unfoldNode(tr.toNode(), tr.toShapeId()))
    )
  }

  // We can only allow dynamic bindings for non-constraint traits, because
  // constraints rely on types (static bindings) to find their refinement providers
  private def unfoldTraitNonConstraint(tr: Trait): Hint = {
    val renderDynamic = model
      .expectShape(tr.toShapeId)
      .hasTrait(
        classOf[smithy4s.meta.RenderAsDynamicBindingTrait]
      ) || smithy4sRenderDynamicHintNamespacePatterns.exists(
      _.matches(tr.toShapeId().namespace)
    )
    if (renderDynamic) Hint.DynamicBinding(tr.toShapeId, tr.toNode)
    else
      Hint.Native(
        tr.toShapeId,
        cats.Eval.later(unfoldNode(tr.toNode(), tr.toShapeId()))
      )
  }

  private def unfoldNodeAndType(layer: NodeAndType): TypedNode[NodeAndType] =
    (layer.node, layer.tpe) match {
      // Struct
      case (N.ObjectNode(map), UnRef(S.Structure(struct))) =>
        val shapeId = struct.getId()
        val ref = Type.Ref(shapeId.getNamespace(), shapeId.getName())
        val structFields = struct.fields
        val fieldNames = struct.fields.map(_.name)
        val fields: List[TypedNode.FieldTN[NodeAndType]] = structFields.map {
          case Field(_, realName, tpe, mod, _, _)
              if mod.typeMod == Field.TypeModification.None =>
            val node = map
              .get(realName)
              .orElse {
                mod.default.map(
                  _.node
                ) // value or default must be present if type is not wrapped
              }
              .map(a => TypedNode.FieldTN.RequiredTN(NodeAndType(a, tpe)))
              .getOrElse(TypedNode.FieldTN.OptionalNoneTN)
            node
          case Field(_, realName, tpe, _, _, _) =>
            map.get(realName) match {
              case Some(node) =>
                TypedNode.FieldTN.OptionalSomeTN(NodeAndType(node, tpe))
              case None => TypedNode.FieldTN.OptionalNoneTN
            }
        }
        TypedNode.StructureTN(ref, fieldNames.zip(fields))
      // Union
      case (N.ObjectNode(map), UnRef(S.Union(union))) =>
        val shapeId = union.getId()
        val ref = Type.Ref(shapeId.getNamespace(), shapeId.getName())
        val (name, node) = map.head // unions are encoded as objects
        val alt = union.getAltTypes.find(_.name == name).get
        val a = if (alt.isAdtMember) {
          val t = NodeAndType(node, alt.tpe)
          TypedNode.AltValueTN.ProductAltTN(t)
        } else if (alt.isUnit) {
          TypedNode.AltValueTN.UnitAltTN
        } else {
          val t = NodeAndType(node, alt.tpe)
          TypedNode.AltValueTN.TypeAltTN(t)
        }
        TypedNode.AltTN(ref, name, a)
      // Alias
      case (node, Type.Alias(ns, name, tpe, _)) =>
        TypedNode.NewTypeTN(Type.Ref(ns, name), NodeAndType(node, tpe))
      case (node, Type.ValidatedAlias(ns, name, tpe)) =>
        TypedNode.ValidatedNewTypeTN(Type.Ref(ns, name), NodeAndType(node, tpe))
      // Enumeration (Enum Trait)
      case (N.StringNode(str), UnRef(shape @ T.enumeration(e))) =>
        val (enumDef, index) =
          e.getValues().asScala.zipWithIndex.find(_._1.getValue() == str).get
        val shapeId = shape.getId()
        val ref = Type.Ref(shapeId.getNamespace(), shapeId.getName())
        TypedNode.EnumerationTN(
          ref,
          enumDef.getValue(),
          index,
          EnumUtil.enumValueClassName(
            enumDef.getName().asScala,
            enumDef.getValue,
            index
          )
        )
      // Enumeration
      case (N.StringNode(str), UnRef(S.Enumeration(enumeration))) =>
        val ((enumName, enumValue), index) =
          enumeration
            .getEnumValues()
            .asScala
            .zipWithIndex
            .find { case ((_, value), _) => value == str }
            .get
        val shapeId = enumeration.getId()
        val ref = Type.Ref(shapeId.getNamespace(), shapeId.getName())
        TypedNode.EnumerationTN(
          ref,
          enumValue,
          index,
          enumName
        )
      // Integer enumeration
      case (N.NumberNode(num), UnRef(S.IntEnumeration(enumeration))) =>
        val (enumName, enumValue) =
          enumeration
            .getEnumValues()
            .asScala
            .find { case (_, value) => value == num.intValue }
            .get
        val shapeId = enumeration.getId()
        val ref = Type.Ref(shapeId.getNamespace(), shapeId.getName())
        TypedNode.EnumerationTN(
          ref,
          enumName,
          enumValue,
          enumName
        )
      // List
      case (
            N.ArrayNode(list),
            Type.Collection(collectionType, mem, _)
          ) =>
        TypedNode.CollectionTN(collectionType, list.map(NodeAndType(_, mem)))
      // Map
      case (N.MapNode(map), Type.Map(mapType, keyType, _, valueType, _)) =>
        TypedNode.MapTN(
          mapType,
          map.map { case (k, v) =>
            (NodeAndType(k, keyType) -> NodeAndType(v, valueType))
          }
        )
      // Primitive
      case (node, Type.PrimitiveType(p)) =>
        unfoldNodeAndTypeP(node, p)
      case (node, Type.Collection(collectionType, _, _))
          if node == Node.nullNode =>
        TypedNode.CollectionTN(collectionType, List.empty)
      case (node, Type.Map(mapType, _, _, _, _)) if node == Node.nullNode =>
        TypedNode.MapTN(mapType, List.empty)
      case (node, IdRefCase()) =>
        val ref = Type.Ref("smithy4s", "ShapeId")
        val namespace :: name :: _ =
          (node.asStringNode.get.getValue.split("#").toList: @unchecked)
        def toField(value: String) = TypedNode.FieldTN.RequiredTN(
          NodeAndType(
            Node.from(value),
            Type.PrimitiveType(Primitive.String)
          )
        )
        TypedNode.StructureTN(
          ref,
          List("namespace" -> toField(namespace), "name" -> toField(name))
        )
      case (node, tpe) => throw UnhandledTraitBinding(node, tpe)
    }

  private object IdRefCase {
    def unapply(tpe: Type): Boolean = tpe match {
      case Type.ExternalType(
            _,
            fqn,
            _,
            _,
            Type.PrimitiveType(Primitive.String),
            _
          ) if fqn === "smithy4s.ShapeId" =>
        true
      case _ => false
    }
  }

  private def unfoldNodeAndTypeP(
      node: Node,
      p: Primitive
  ): TypedNode[NodeAndType] = {
    def notSupported(nodeAndPrimitive: (Node, Primitive)) =
      throw new NotImplementedError(
        s"Unsupported case: $nodeAndPrimitive"
      )
    (node, p) match {
      // String
      case (N.StringNode(str), Primitive.String) =>
        TypedNode.PrimitiveTN(Primitive.String, Some(str))
      // Numeric
      case (N.NumberNode(num), Primitive.Int) =>
        TypedNode.PrimitiveTN(Primitive.Int, Some(num.intValue()))
      case (N.NumberNode(num), Primitive.Long) =>
        TypedNode.PrimitiveTN(Primitive.Long, Some(num.longValue()))
      case (N.NumberNode(num), Primitive.Double) =>
        TypedNode.PrimitiveTN(Primitive.Double, Some(num.doubleValue()))
      case (N.NumberNode(num), Primitive.Float) =>
        TypedNode.PrimitiveTN(Primitive.Float, Some(num.floatValue()))
      case (N.NumberNode(num), Primitive.Short) =>
        TypedNode.PrimitiveTN(Primitive.Short, Some(num.shortValue()))
      case (N.NumberNode(num), Primitive.BigDecimal) =>
        TypedNode.PrimitiveTN(
          Primitive.BigDecimal,
          Some(BigDecimal(num.doubleValue()))
        )
      case (N.NumberNode(num), Primitive.BigInteger) =>
        TypedNode.PrimitiveTN(
          Primitive.BigInteger,
          Some(BigInt(num.intValue()))
        )
      // Boolean
      case (N.BooleanNode(bool), Primitive.Bool) =>
        TypedNode.PrimitiveTN(Primitive.Bool, Some(bool))
      case (node, Primitive.Document) =>
        TypedNode.PrimitiveTN(Primitive.Document, Some(node))
      case (node, Primitive.String) if node == Node.nullNode =>
        TypedNode.PrimitiveTN(Primitive.String, None)
      case (node, Primitive.Int) if node == Node.nullNode =>
        TypedNode.PrimitiveTN(Primitive.Int, None)
      case (node, Primitive.Long) if node == Node.nullNode =>
        TypedNode.PrimitiveTN(Primitive.Long, None)
      case (node, Primitive.Double) if node == Node.nullNode =>
        TypedNode.PrimitiveTN(Primitive.Double, None)
      case (node, Primitive.Float) if node == Node.nullNode =>
        TypedNode.PrimitiveTN(Primitive.Float, None)
      case (node, Primitive.Short) if node == Node.nullNode =>
        TypedNode.PrimitiveTN(Primitive.Short, None)
      case (node, Primitive.Byte) if node == Node.nullNode =>
        TypedNode.PrimitiveTN(Primitive.Byte, None)
      case (node, Primitive.Blob) if node == Node.nullNode =>
        TypedNode.PrimitiveTN(Primitive.Blob, None)
      case (node, Primitive.Bool) if node == Node.nullNode =>
        TypedNode.PrimitiveTN(Primitive.Bool, None)
      case timestamp @ (node, Primitive.Timestamp) =>
        val value = node match {
          case N.StringNode(str) =>
            Some(
              Try(Instant.parse(str))
                .orElse(
                  Try(ZonedDateTime.parse(str, httpDateFormatter).toInstant())
                )
                .toOption
                .getOrElse(notSupported(timestamp))
            )
          case N.NumberNode(num) =>
            Some(Instant.ofEpochSecond(num.longValue))
          case _ if node == Node.nullNode => None
          case _                          => notSupported(timestamp)
        }
        TypedNode.PrimitiveTN(Primitive.Timestamp, value)
      case (_, Primitive.Unit) =>
        TypedNode.PrimitiveTN(
          Primitive.Unit,
          None
        )
      case (node, Primitive.Uuid) if node == Node.nullNode =>
        TypedNode.PrimitiveTN(Primitive.Uuid, None)
      case (N.StringNode(s), Primitive.Uuid) =>
        Try(UUID.fromString(s))
          .map(uuid => TypedNode.PrimitiveTN(Primitive.Uuid, Some(uuid)))
          .adaptErr { case e =>
            new Exception(
              s"UUID failed validation at codegen time. Defined at: ${node.getSourceLocation()}",
              e
            )
          }
          .get
      case other =>
        notSupported(other)
    }
  }

  private val httpDateFormatter = new DateTimeFormatterBuilder()
    .appendPattern("EEE, dd MMM yyyy HH:mm:ss z")
    .parseDefaulting(ChronoField.OFFSET_SECONDS, 0)
    .toFormatter(Locale.ENGLISH);

}
