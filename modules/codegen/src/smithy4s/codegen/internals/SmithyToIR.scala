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

package smithy4s.codegen.internals

import cats.data.NonEmptyList
import cats.implicits._
import smithy4s.meta.AdtMemberTrait
import smithy4s.meta.ErrorMessageTrait
import smithy4s.meta.IndexedSeqTrait
import smithy4s.meta.PackedInputsTrait
import smithy4s.meta.RefinementTrait
import smithy4s.meta.VectorTrait
import smithy4s.meta.AdtTrait
import software.amazon.smithy.aws.traits.ServiceTrait
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.node._
import software.amazon.smithy.model.selector.PathFinder
import software.amazon.smithy.model.shapes._
import software.amazon.smithy.model.traits.DefaultTrait
import software.amazon.smithy.model.traits.{RequiredTrait, TimestampFormatTrait}
import software.amazon.smithy.model.traits._

import scala.annotation.nowarn
import scala.jdk.CollectionConverters._

import Type.Alias

private[codegen] object SmithyToIR {

  def apply(model: Model, namespace: String): CompilationUnit = {
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

private[codegen] class SmithyToIR(model: Model, namespace: String) {

  val finder = PathFinder.create(model)

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

  def allDecls = allShapes
    .filter(_.getId().getNamespace() == namespace)
    .flatMap(_.accept(toIRVisitor(renderAdtMemberStructures = false)))
    .toList

  def toIRVisitor(
      renderAdtMemberStructures: Boolean
  ): ShapeVisitor[Option[Decl]] =
    new ShapeVisitor[Option[Decl]] {

      private def getDefault(shape: Shape): Option[Decl] = {
        val hints = SmithyToIR.this.hints(shape)

        val recursive = hints.exists {
          case Hint.Trait => true
          case _          => false
        }

        shape.tpe.flatMap {
          case Type.Alias(_, name, tpe: Type.ExternalType, isUnwrapped) =>
            val newHints = hints.filterNot(_ == tpe.refinementHint)
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

      override def blobShape(x: BlobShape): Option[Decl] = getDefault(x)

      override def booleanShape(x: BooleanShape): Option[Decl] = getDefault(x)

      override def listShape(x: ListShape): Option[Decl] = getDefault(x)

      @annotation.nowarn("msg=class SetShape in package shapes is deprecated")
      override def setShape(x: SetShape): Option[Decl] = getDefault(x)

      override def mapShape(x: MapShape): Option[Decl] = getDefault(x)

      override def byteShape(x: ByteShape): Option[Decl] = getDefault(x)

      override def shortShape(x: ShortShape): Option[Decl] = getDefault(x)

      override def integerShape(x: IntegerShape): Option[Decl] = getDefault(x)

      override def longShape(x: LongShape): Option[Decl] = getDefault(x)

      override def floatShape(x: FloatShape): Option[Decl] = getDefault(x)

      override def documentShape(x: DocumentShape): Option[Decl] = getDefault(x)

      override def doubleShape(x: DoubleShape): Option[Decl] = getDefault(x)

      override def bigIntegerShape(x: BigIntegerShape): Option[Decl] =
        getDefault(x)

      override def bigDecimalShape(x: BigDecimalShape): Option[Decl] =
        getDefault(x)

      override def operationShape(x: OperationShape): Option[Decl] = getDefault(
        x
      )

      override def resourceShape(x: ResourceShape): Option[Decl] = getDefault(x)

      override def memberShape(x: MemberShape): Option[Decl] = None

      override def timestampShape(x: TimestampShape): Option[Decl] = getDefault(
        x
      )

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
              val memberOptional = !member.hasTrait(classOf[RequiredTrait])
              val memberHasDefault = member.hasTrait(classOf[DefaultTrait])

              // if member has no default and is optional, then the field must be optional
              if (!memberHasDefault && memberOptional) !field.required
              else field.required
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
        val filteredMixins = shape
          .getMixins()
          .asScala
          .filter(mixinId => doFieldsMatch(mixinId, fields))
        val mixins = filterMixinsExistOnParentAdt(filteredMixins.toSet, shape)
          .flatMap(_.tpe)
          .toList
        val isMixin = shape.hasTrait(classOf[MixinTrait])

        val p =
          Product(
            shape.getId(),
            shape.name,
            fields,
            mixins,
            rec,
            hints,
            isMixin
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
        val mixins = memberTargets
          .map(_.getMixins.asScala.toSet)

        val union = mixins.foldLeft(Set.empty[ShapeId])(_ union _)

        val result = mixins.foldLeft(union)(_ intersect _)

        result.toList
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
          case T.enumeration(e) =>
            val values = e
              .getValues()
              .asScala
              .zipWithIndex
              .map { case (value, index) =>
                EnumValue(
                  value.getValue(),
                  index,
                  EnumUtil.enumValueClassName(
                    value.getName().asScala,
                    value.getValue,
                    index
                  ),
                  hints = Nil
                )
              }
              .toList
            Enumeration(shape.getId(), shape.name, values, hints(shape)).some
          case _ => this.getDefault(shape)
        })

      override def enumShape(shape: EnumShape): Option[Decl] = {
        val values = shape
          .getEnumValues()
          .asScala
          .zipWithIndex
          .map { case ((name, value), index) =>
            val member = shape.getMember(name).get()

            EnumValue(value, index, name, hints(member))
          }
          .toList

        Enumeration(
          shape.getId(),
          shape.name,
          values,
          hints = hints(shape)
        ).some
      }

      override def intEnumShape(shape: IntEnumShape): Option[Decl] = {
        val values = shape
          .getEnumValues()
          .asScala
          .map { case (name, value) =>
            val member = shape.getMember(name).get()

            EnumValue(name, value, name, hints(member))
          }
          .toList
        Enumeration(
          shape.getId(),
          shape.name,
          values,
          hints(shape) :+ Hint.IntEnum
        ).some
      }

      override def serviceShape(shape: ServiceShape): Option[Decl] = {
        val generalErrors: List[Type] =
          shape
            .getErrors()
            .asScala
            .toList
            .map(_.tpe)
            .collect { case Some(tpe) => tpe }

        // Aggregates both the operations of the current entity and the ones
        // in the sub-entities.
        def recursiveOperations(
            entity: EntityShape
        ): List[ShapeId] = {
          entity
            .getAllOperations()
            .asScala
            .toList ++ entity.getResources().asScala.flatMap { shapeId =>
            recursiveOperations(
              model.expectShape(shapeId, classOf[EntityShape])
            )
          }
        }

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

            val errorTypes = (generalErrors ++ op
              .getErrors()
              .asScala
              .map(_.tpe)
              .collect { case Some(errorType) =>
                errorType
              }
              .toList).distinct

            val outputType =
              op.getOutputShape().tpe.getOrElse(Type.unit)

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
              hints(op)
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

  private def addedTraits(
      traits: java.util.Collection[Trait]
  ): ShapeVisitor[Shape] =
    new ShapeVisitor[Shape] {
      //format: off
      def blobShape(x: BlobShape): Shape = x.toBuilder().addTraits(traits).build()
      def booleanShape(x: BooleanShape): Shape = x.toBuilder().addTraits(traits).build()
      def listShape(x: ListShape): Shape = x.toBuilder().addTraits(traits).build()
      @nowarn("msg=class SetShape in package shapes is deprecated")
      override def setShape(x: SetShape): Shape = x.toBuilder().addTraits(traits).build()
      def mapShape(x: MapShape): Shape = x.toBuilder().addTraits(traits).build()
      def byteShape(x: ByteShape): Shape = x.toBuilder().addTraits(traits).build()
      def shortShape(x: ShortShape): Shape = x.toBuilder().addTraits(traits).build()
      def integerShape(x: IntegerShape): Shape = x.toBuilder().addTraits(traits).build()
      def longShape(x: LongShape): Shape = x.toBuilder().addTraits(traits).build()
      def floatShape(x: FloatShape): Shape = x.toBuilder().addTraits(traits).build()
      def documentShape(x: DocumentShape): Shape = x.toBuilder().addTraits(traits).build()
      def doubleShape(x: DoubleShape): Shape = x.toBuilder().addTraits(traits).build()
      def bigIntegerShape(x: BigIntegerShape): Shape = x.toBuilder().addTraits(traits).build()
      def bigDecimalShape(x: BigDecimalShape): Shape = x.toBuilder().addTraits(traits).build()
      def operationShape(x: OperationShape): Shape = x.toBuilder().addTraits(traits).build()
      def resourceShape(x: ResourceShape): Shape = x.toBuilder().addTraits(traits).build()
      def serviceShape(x: ServiceShape): Shape = x.toBuilder().addTraits(traits).build()
      def stringShape(x: StringShape): Shape = x.toBuilder().addTraits(traits).build()
      def structureShape(x: StructureShape): Shape = x.toBuilder().addTraits(traits).build()
      def unionShape(x: UnionShape): Shape = x.toBuilder().addTraits(traits).build()
      def memberShape(x: MemberShape): Shape = x.toBuilder().addTraits(traits).build()
      def timestampShape(x: TimestampShape): Shape = x.toBuilder().addTraits(traits).build()
      //format: on
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

      private def getExternalTypeInfo(
          shape: Shape
      ): Option[(Trait, RefinementTrait)] = {
        shape
          .getAllTraits()
          .asScala
          .flatMap { case (_, trt) =>
            model
              .getShape(trt.toShapeId)
              .asScala
              .flatMap(_.getTrait(classOf[RefinementTrait]).asScala)
              .map(trt -> _)
          }
          .headOption // Shapes can have at most ONE trait that has the refined trait
      }

      private def getExternalOrBase(shape: Shape, base: Type): Type = {
        getExternalTypeInfo(shape)
          .map { case (trt, refined) =>
            val baseTypeParams = base match {
              case c: Type.Collection => List(c.member)
              case m: Type.Map        => List(m.key, m.value)
              case other              => List(other)
            }
            Type.ExternalType(
              shape.name,
              refined.getTargetType(),
              if (refined.isParameterised) baseTypeParams else List.empty,
              refined.getProviderImport().asScala,
              base,
              unfoldTrait(trt)
            )
          }
          .getOrElse(base)
      }

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
          Type
            .Alias(
              shape.getId().getNamespace(),
              shape.getId().getName(),
              externalOrBase,
              isUnwrappedShape(shape)
            )
            .some
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
          primitive(x, "smithy.api#Blob", Primitive.ByteArray)
        }

      def booleanShape(x: BooleanShape): Option[Type] =
        primitive(x, "smithy.api#Boolean", Primitive.Bool)

      def listShape(x: ListShape): Option[Type] =
        x.getMember()
          .accept(this)
          .map { tpe =>
            val _hints = hints(x)
            val memberHints = hints(x.getMember())
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

      @nowarn("msg=class SetShape in package shapes is deprecated")
      override def setShape(x: SetShape): Option[Type] =
        x.getMember()
          .accept(this)
          .map(Type.Collection(CollectionType.Set, _, hints(x.getMember())))
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

      def mapShape(x: MapShape): Option[Type] = (for {
        k <- x.getKey().accept(this)
        v <- x.getValue().accept(this)
      } yield Type.Map(k, hints(x.getKey()), v, hints(x.getValue()))).map {
        tpe =>
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

      def bigDecimalShape(x: BigDecimalShape): Option[Type] =
        primitive(x, "smithy.api#BigDecimal", Primitive.BigDecimal)

      def operationShape(x: OperationShape): Option[Type] = None

      def resourceShape(x: ResourceShape): Option[Type] = None

      def serviceShape(x: ServiceShape): Option[Type] = None

      override def enumShape(x: EnumShape): Option[Type] =
        Type.Ref(x.namespace, x.name).some

      def stringShape(x: StringShape): Option[Type] = x match {
        case T.enumeration(_) => Type.Ref(x.namespace, x.name).some
        case shape if shape.getId() == uuidShapeId =>
          Type.PrimitiveType(Primitive.Uuid).some
        case T.uuidFormat(_) =>
          Type
            .Alias(
              x.namespace,
              x.name,
              Type.PrimitiveType(Primitive.Uuid),
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

      def memberShape(x: MemberShape): Option[Type] =
        model.getShape(x.getTarget()).asScala.flatMap { shape =>
          shape
            .accept(
              addedTraits(x.getAllTraits().asScala.map(_._2).asJavaCollection)
            )
            .accept(this)
        }

      def timestampShape(x: TimestampShape): Option[Type] =
        primitive(x, "smithy.api#Timestamp", Primitive.Timestamp)

    }

  private def imputeZeroValuesOnDefaultTraits(
      shape: Shape
  ): List[Trait] = shape.getAllTraits().asScala.values.toList.map {
    case default: DefaultTrait if default.toNode == Node.nullNode =>
      val tpe = shape.asMemberShape().asScala match {
        case Some(memShape) => model.getShape(memShape.getTarget).get.getType
        case None           => shape.getType
      }
      val newNode = tpe match {
        case ShapeType.STRING      => Node.from("")
        case ShapeType.MAP         => Node.objectNode()
        case ShapeType.LIST        => Node.arrayNode()
        case ShapeType.INTEGER     => Node.from(0)
        case ShapeType.BIG_DECIMAL => Node.from(0)
        case ShapeType.BIG_INTEGER => Node.from(0)
        case ShapeType.LONG        => Node.from(0L)
        case ShapeType.DOUBLE      => Node.from(0.0d)
        case ShapeType.SHORT       => Node.from(0: Short)
        case ShapeType.FLOAT       => Node.from(0.0f)
        case ShapeType.BOOLEAN     => Node.from(false)
        case ShapeType.BLOB        => Node.arrayNode()
        case ShapeType.BYTE        => Node.from(0)
        case ShapeType.TIMESTAMP =>
          shape
            .getTrait(classOf[TimestampFormatTrait])
            .asScala
            .map(_.getValue) match {
            case Some(TimestampFormatTrait.DATE_TIME) =>
              Node.from("1970-01-01T00:00:00.00Z")
            case Some(TimestampFormatTrait.HTTP_DATE) =>
              Node.from("Thu, 01 Jan 1970 00:00:00 GMT")
            case _ => Node.from(0)
          }
        case _ => default.toNode
      }
      new DefaultTrait(newNode)
    case other => other
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
  private def maybeDefault(shape: MemberShape): List[Hint.Default] = {
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
      val targetTpe = shape.getTarget.tpe.get
      // Constructing the initial value for the refold
      val nodeAndType = targetTpe match {
        case Alias(_, _, tpe, true) => NodeAndType(tr.toNode(), tpe)
        case _                      => NodeAndType(tr.toNode(), targetTpe)
      }
      val maybeTree =
        recursion.anaM(unfoldNodeAndTypeIfNotExternal)(nodeAndType)
      maybeTree.map(Hint.Default(_)).toList
    } else {
      List.empty
    }
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
    case _: VectorTrait =>
      Hint.SpecializedList.Vector
    case _: IndexedSeqTrait =>
      Hint.SpecializedList.IndexedSeq
    case _: UniqueItemsTrait =>
      Hint.UniqueItems
    case t if t.toShapeId() == ShapeId.fromParts("smithy.api", "trait") =>
      Hint.Trait
    case ConstraintTrait(tr) => Hint.Constraint(toTypeRef(tr), unfoldTrait(tr))
  }

  private def documentationHint(shape: Shape): Option[Hint] = {
    def split(s: String) =
      s.replace("*/", "\\*\\/").linesIterator.toList
    val shapeDocs = shape
      .getTrait(classOf[DocumentationTrait])
      .asScala
      .foldMap(doc => split(doc.getValue()))
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
    if (shapeDocs.nonEmpty || memberDocs.nonEmpty) {
      Some(Hint.Documentation(shapeDocs, memberDocs))
    } else None
  }

  private def hints(shape: Shape): List[Hint] = {
    val traits = imputeZeroValuesOnDefaultTraits(shape)
    val nonMetaTraits =
      traits
        .filterNot(_.toShapeId().getNamespace() == "smithy4s.meta")
        // traits from the synthetic namespace, e.g. smithy.synthetic.enum
        // don't have shapes in the model - so we can't generate hints for them.
        .filterNot(_.toShapeId().getNamespace() == "smithy.synthetic")
        // enumValue can be derived from enum schemas anyway, so we're removing it from hints
        .filterNot(_.toShapeId() == EnumValueTrait.ID)

    val nonConstraintNonMetaTraits = nonMetaTraits.collect {
      case t if ConstraintTrait.unapply(t).isEmpty => t
    }
    traits.collect(traitToHint(shape)) ++
      documentationHint(shape) ++
      nonConstraintNonMetaTraits.map(unfoldTrait)
  }

  case class AltInfo(name: String, tpe: Type, isAdtMember: Boolean)

  implicit class ShapeExt(shape: Shape) {
    def name = shape.getId().getName()

    def namespace = shape.getId().getNamespace()

    def tpe: Option[Type] = shape.accept(toType)

    def fields = {
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
              maybeDefault(member)
            else List.empty
          val defaultable = member.hasTrait(classOf[DefaultTrait]) &&
            !member.tpe.exists(_.isExternal)
          (
            member.getMemberName(),
            member.tpe,
            member.hasTrait(classOf[RequiredTrait]) || defaultable,
            hints(member) ++ default ++ noDefault
          )
        }
        .collect { case (name, Some(tpe), required, hints) =>
          Field(name, tpe, required, hints)
        }
        .toList

      val hintsContainsDefault: Field => Boolean = f =>
        f.hints.exists {
          case _: Hint.Default => true
          case _               => false
        }

      defaultRenderMode match {
        case DefaultRenderMode.Full =>
          result.sortBy(hintsContainsDefault).sortBy(!_.required)
        case DefaultRenderMode.OptionOnly =>
          result.sortBy(!_.required)
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
        .map { member =>
          val memberTarget =
            model.expectShape(member.getTarget)
          if (isPartOfAdt(memberTarget)) {
            (member.getMemberName(), member.tpe.map(Left(_)))
          } else {
            (member.getMemberName(), member.tpe.map(Right(_)))
          }
        }
        .collect {
          case (name, Some(Left(tpe))) =>
            AltInfo(name, tpe, isAdtMember = true)
          case (name, Some(Right(tpe))) =>
            AltInfo(name, tpe, isAdtMember = false)
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
                cId.getNamespace + "." + parent.getName
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

  private def unfoldTrait(tr: Trait): Hint.Native = {
    val nodeAndType = NodeAndType(tr.toNode(), tr.toShapeId().tpe.get)
    Hint.Native(recursion.ana(unfoldNodeAndType)(nodeAndType))
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
          case Field(_, realName, tpe, true, _) =>
            val node = map.get(realName).getOrElse {
              struct
                .getMember(realName)
                .get
                .getTrait(classOf[DefaultTrait])
                .get
                .toNode
            } // value or default must be present on required field
            TypedNode.FieldTN.RequiredTN(NodeAndType(node, tpe))
          case Field(_, realName, tpe, false, _) =>
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
        } else {
          val t = NodeAndType(node, alt.tpe)
          TypedNode.AltValueTN.TypeAltTN(t)
        }
        TypedNode.AltTN(ref, name, a)
      // Alias
      case (node, Type.Alias(ns, name, tpe, _)) =>
        TypedNode.NewTypeTN(Type.Ref(ns, name), NodeAndType(node, tpe))
      // Enumeration
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
      // List
      case (
            N.ArrayNode(list),
            Type.Collection(collectionType, mem, _)
          ) =>
        TypedNode.CollectionTN(collectionType, list.map(NodeAndType(_, mem)))
      // Map
      case (N.MapNode(map), Type.Map(keyType, _, valueType, _)) =>
        TypedNode.MapTN(map.map { case (k, v) =>
          (NodeAndType(k, keyType) -> NodeAndType(v, valueType))
        })
      // Primitive
      case (node, Type.PrimitiveType(p)) =>
        unfoldNodeAndTypeP(node, p)
      case (node, Type.Collection(collectionType, _, _))
          if node == Node.nullNode =>
        TypedNode.CollectionTN(collectionType, List.empty)
      case (node, Type.Map(_, _, _, _)) if node == Node.nullNode =>
        TypedNode.MapTN(List.empty)
      case (node, tpe) => throw UnhandledTraitBinding(node, tpe)
    }

  private def unfoldNodeAndTypeP(
      node: Node,
      p: Primitive
  ): TypedNode[NodeAndType] = (node, p) match {
    // String
    case (N.StringNode(str), Primitive.String) =>
      TypedNode.PrimitiveTN(Primitive.String, str)
    // Numeric
    case (N.NumberNode(num), Primitive.Int) =>
      TypedNode.PrimitiveTN(Primitive.Int, num.intValue())
    case (N.NumberNode(num), Primitive.Long) =>
      TypedNode.PrimitiveTN(Primitive.Long, num.longValue())
    case (N.NumberNode(num), Primitive.Double) =>
      TypedNode.PrimitiveTN(Primitive.Double, num.doubleValue())
    case (N.NumberNode(num), Primitive.Float) =>
      TypedNode.PrimitiveTN(Primitive.Float, num.floatValue())
    case (N.NumberNode(num), Primitive.Short) =>
      TypedNode.PrimitiveTN(Primitive.Short, num.shortValue())
    case (N.NumberNode(num), Primitive.BigDecimal) =>
      TypedNode.PrimitiveTN(Primitive.BigDecimal, BigDecimal(num.doubleValue()))
    case (N.NumberNode(num), Primitive.BigInteger) =>
      TypedNode.PrimitiveTN(Primitive.BigInteger, BigInt(num.intValue()))
    // Boolean
    case (N.BooleanNode(bool), Primitive.Bool) =>
      TypedNode.PrimitiveTN(Primitive.Bool, bool)
    case (node, Primitive.Document) =>
      TypedNode.PrimitiveTN(Primitive.Document, node)
    case (node, Primitive.String) if node == Node.nullNode =>
      TypedNode.PrimitiveTN(Primitive.String, "")
    case (node, Primitive.Int) if node == Node.nullNode =>
      TypedNode.PrimitiveTN(Primitive.Int, 0)
    case (node, Primitive.Long) if node == Node.nullNode =>
      TypedNode.PrimitiveTN(Primitive.Long, 0L)
    case (node, Primitive.Double) if node == Node.nullNode =>
      TypedNode.PrimitiveTN(Primitive.Double, 0.0)
    case (node, Primitive.Float) if node == Node.nullNode =>
      TypedNode.PrimitiveTN(Primitive.Float, 0.0f)
    case (node, Primitive.Short) if node == Node.nullNode =>
      TypedNode.PrimitiveTN(Primitive.Short, 0: Short)
    case (node, Primitive.Byte) if node == Node.nullNode =>
      TypedNode.PrimitiveTN(Primitive.Byte, 0.toByte)
    case (node, Primitive.ByteArray) if node == Node.nullNode =>
      TypedNode.PrimitiveTN(Primitive.ByteArray, Array.empty[Byte])
    case (node, Primitive.Bool) if node == Node.nullNode =>
      TypedNode.PrimitiveTN(Primitive.Bool, false)
    case (node, Primitive.Timestamp) if node == Node.nullNode =>
      TypedNode.PrimitiveTN(
        Primitive.Timestamp,
        java.time.Instant.ofEpochSecond(0)
      )
    case other =>
      throw new NotImplementedError(s"Unsupported case: $other")
  }

}
