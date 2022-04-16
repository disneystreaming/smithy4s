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

package smithy4s.codegen

import cats.data.NonEmptyList
import cats.implicits._
import smithy4s.meta.PackedInputsTrait
import smithy4s.recursion._
import software.amazon.smithy.aws.traits.ServiceTrait
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes._
import software.amazon.smithy.model.traits.RequiredTrait
import software.amazon.smithy.model.traits._

import scala.jdk.CollectionConverters._

object SmithyToIR {

  def apply(model: Model, namespace: String): CompilationUnit = {
    PostProcessor(
      CompilationUnit(namespace, new SmithyToIR(model, namespace).allDecls)
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

  val allShapes =
    model
      .shapes()
      .iterator()
      .asScala
      .toList

  def allDecls = allShapes
    .filter(_.getId().getNamespace() == namespace)
    .map(_.accept(toIRVisitor))
    .collect { case Some(decl) =>
      decl
    }
    .toList

  val toIRVisitor: ShapeVisitor[Option[Decl]] =
    new ShapeVisitor.Default[Option[Decl]] {
      def getDefault(shape: Shape): Option[Decl] = {
        val hints = traitsToHints(shape.getAllTraits().asScala.values.toList)

        if (shape.isMemberShape()) None
        else
          shape.tpe.flatMap {
            case Type.Alias(_, name, tpe) =>
              TypeAlias(name, name, tpe, hints).some
            case Type.PrimitiveType(_) => None
            case other => TypeAlias(shape.name, shape.name, other, hints).some
          }
      }

      override def structureShape(shape: StructureShape): Option[Decl] = {
        val rec = isRecursive(shape.getId(), Set.empty)

        val hints = traitsToHints(shape.getAllTraits().asScala.values.toList)
        Product(shape.name, shape.name, shape.fields, rec, hints).some
      }

      override def unionShape(shape: UnionShape): Option[Decl] = {
        val rec = isRecursive(shape.getId(), Set.empty)

        val hints = traitsToHints(shape.getAllTraits().asScala.values.toList)
        NonEmptyList.fromList(shape.alts).map { case alts =>
          Union(shape.name, shape.name, alts, rec, hints)
        }
      }

      override def enumShape(shape: EnumShape): Option[Decl] = {
        val values = shape
          .getEnumValues()
          .asScala
          .zipWithIndex
          .map { case ((name, value), index) =>
            EnumValue(value, index, name)
          }
          .toList
        Enumeration(shape.name, shape.name, values).some
      }

      override def serviceShape(shape: ServiceShape): Option[Decl] = {
        val generalErrors: List[Type] =
          shape
            .getErrors()
            .asScala
            .toList
            .map(_.tpe)
            .collect { case Some(tpe) => tpe }

        val operations = shape
          .getAllOperations()
          .asScala
          .toList
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
              op.name,
              op.namespace,
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
          prettyName,
          shape.name,
          operations,
          serviceHints,
          shape.getVersion()
        ).some
      }
    }

  private def isRecursive(id: ShapeId, visited: Set[ShapeId]): Boolean =
    if (visited.contains(id)) true
    else
      model
        .getShape(id)
        .asScala
        .toList
        .flatMap(_.members().asScala.toList)
        .map(_.getTarget())
        .exists(isRecursive(_, visited + id))

  private def addedTraits(
      traits: java.util.Collection[Trait]
  ): ShapeVisitor[Shape] =
    new ShapeVisitor[Shape] {
      //format: off
      def blobShape(x: BlobShape): Shape = x.toBuilder().addTraits(traits).build()
      def booleanShape(x: BooleanShape): Shape = x.toBuilder().addTraits(traits).build()
      def listShape(x: ListShape): Shape = x.toBuilder().addTraits(traits).build()
      def setShape(x: SetShape): Shape = x.toBuilder().addTraits(traits).build()
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

      def primitive(
          shape: Shape,
          primitiveId: String,
          primitive: Primitive
      ): Option[Type] = {
        if (
          shape.getId() != ShapeId
            .from(primitiveId) && !isUnboxedPrimitive(shape.getId())
        ) {
          Type
            .Alias(
              shape.getId().getNamespace(),
              shape.getId().getName(),
              Type.PrimitiveType(primitive)
            )
            .some
        } else Type.PrimitiveType(primitive).some
      }

      def blobShape(x: BlobShape): Option[Type] =
        if (x.getTrait(classOf[StreamingTrait]).isPresent()) {
          Type
            .Alias(
              x.getId().getNamespace(),
              x.getId().getName,
              Type.PrimitiveType(Primitive.Byte)
            )
            .some
        } else {
          primitive(x, "smithy.api#Blob", Primitive.ByteArray)
        }

      def booleanShape(x: BooleanShape): Option[Type] =
        primitive(x, "smithy.api#Boolean", Primitive.Bool)

      def listShape(x: ListShape): Option[Type] =
        x.getMember().accept(this).map(Type.List.apply).map { tpe =>
          Type.Alias(x.namespace, x.name, tpe)
        }

      def setShape(x: SetShape): Option[Type] =
        x.getMember().accept(this).map(Type.Set.apply).map { tpe =>
          Type.Alias(x.namespace, x.name, tpe)
        }

      def mapShape(x: MapShape): Option[Type] = (for {
        k <- x.getKey().accept(this)
        v <- x.getValue().accept(this)
      } yield Type.Map(k, v)).map { tpe =>
        Type.Alias(x.namespace, x.name, tpe)
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
        case shape if shape.getId() == uuidShapeId =>
          Type.PrimitiveType(Primitive.Uuid).some
        case T.uuidFormat(_) =>
          Type
            .Alias(x.namespace, x.name, Type.PrimitiveType(Primitive.Uuid))
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

  private def hints(shape: Shape): List[Hint] = traitsToHints(
    shape.getAllTraits().asScala.values.toList
  )

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

  private val traitToHint: PartialFunction[Trait, Hint] = {
    case _: ErrorTrait => Hint.Error
    case t: ProtocolDefinitionTrait =>
      val shapeIds = t.getTraits()
      val refs = shapeIds.asScala.map(shapeId =>
        Type.Ref(shapeId.getNamespace(), shapeId.getName())
      )
      Hint.Protocol(refs.toList)
    case _: PackedInputsTrait =>
      Hint.PackedInputs
    case t if t.toShapeId() == ShapeId.fromParts("smithy.api", "trait") =>
      Hint.Trait
    case ConstraintTrait(tr) => Hint.Constraint(toTypeRef(tr))
  }

  private def traitsToHints(traits: List[Trait]): List[Hint] = {
    val nonMetaTraits =
      traits.filterNot(_.toShapeId().getNamespace() == "smithy4s.meta")
    traits.collect(traitToHint) ++ nonMetaTraits.map(unfoldTrait)
  }

  implicit class ShapeExt(shape: Shape) {
    def name = shape.getId().getName()

    def namespace = shape.getId().getNamespace()

    def tpe: Option[Type] = shape.accept(toType)

    def fields = shape
      .members()
      .asScala
      .filterNot(isStreaming)
      .map { member =>
        (
          member.getMemberName(),
          member.tpe,
          member.hasTrait(classOf[RequiredTrait]),
          hints(member)
        )
      }
      .collect { case (name, Some(tpe), required, hints) =>
        Field(name, tpe, required, hints)
      }
      .toList
      .sortBy(!_.required)

    def alts =
      shape
        .members()
        .asScala
        .map { member =>
          (member.getMemberName(), member.tpe, hints(member))
        }
        .collect { case (name, Some(tpe), h) =>
          Alt(name, tpe, h)
        }
        .toList

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

  private object UnRef {
    def unapply(tpe: Type): Option[Shape] = tpe match {
      case Type.Ref(ns, name) =>
        model
          .getShape(ShapeId.fromParts(ns, name))
          .asScala
      case _ => None
    }
  }

  case class UnhandledTraitBinding(node: Node, tpe: Type) extends Throwable {
    override def getMessage(): String =
      s"Unhandled trait binding:\ntype: $tpe\nvalue: ${Node.printJson(node)}"
  }

  private def unfoldTrait(tr: Trait): Hint = {
    val nodeAndType = NodeAndType(tr.toNode(), tr.toShapeId().tpe.get)
    Hint.Native(ana(unfoldNodeAndType)(nodeAndType))
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
            val node = map(realName) // validated by smithy
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
        val alt = union.alts.find(_.name == name).get
        TypedNode.AltTN(ref, name, NodeAndType(node, alt.tpe))
      // Alias
      case (node, Type.Alias(ns, name, tpe)) =>
        TypedNode.NewTypeTN(Type.Ref(ns, name), NodeAndType(node, tpe))
      // Enumeration
      case (N.StringNode(str), UnRef(S.Enumeration(enumeration))) =>
        val ((name, value), index) =
          enumeration
            .getEnumValues()
            .asScala
            .zipWithIndex
            .find { case ((_, name), _) => name == str }
            .get
        val shapeId = enumeration.getId()
        val ref = Type.Ref(shapeId.getNamespace(), shapeId.getName())
        TypedNode.EnumerationTN(
          ref,
          value,
          index,
          name
        )
      // List
      case (N.ArrayNode(list), Type.List(mem)) =>
        TypedNode.ListTN(list.map(NodeAndType(_, mem)))
      // Set
      case (N.ArrayNode(set), Type.Set(mem)) =>
        TypedNode.SetTN(set.map(NodeAndType(_, mem)))
      // Map
      case (N.MapNode(map), Type.Map(keyType, valueType)) =>
        TypedNode.MapTN(map.map { case (k, v) =>
          (NodeAndType(k, keyType) -> NodeAndType(v, valueType))
        })
      // Primitive
      case (node, Type.PrimitiveType(p)) =>
        unfoldNodeAndTypeP(node, p)
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
    case other =>
      throw new NotImplementedError(s"Unsupported case : $other")
  }

}
