package smithy4s
package dynamic

import software.amazon.smithy.model.Model
import scala.collection.mutable.{Map => MMap}
import schematic.OneOf
import schematic.StructureField
import software.amazon.smithy.model.shapes._
import smithy4s.syntax._
import scala.jdk.CollectionConverters._
import software.amazon.smithy.model.traits._
import software.amazon.smithy.model.traits
import software.amazon.smithy.model.node.Node
import java.util.Optional

object Compiler {

  type DynSchema = Schema[DynData]
  type DynFieldSchema = StructureField[Schematic, DynStruct, DynData]
  type DynAltSchema = OneOf[Schematic, DynAlt, DynData]

  def compile(model: Model, knownHints: KeyedSchema[_]*): DynamicModel = {
    val schemaMap = MMap.empty[ShapeId, Schema[DynData]]
    val endpointMap = MMap.empty[ShapeId, DynamicEndpoint]
    val serviceMap = MMap.empty[ShapeId, DynamicService]

    val hintsMap: Map[ShapeId, KeyedSchema[_]] = knownHints.map { ks =>
      val shapeId: ShapeId =
        ShapeId.fromParts(ks.hintKey.namespace, ks.hintKey.name)
      shapeId -> ks
    }.toMap

    def toHintAux[A](ks: KeyedSchema[A], node: Node): Option[Hint] = {
      val documentRepr: Document = NodeToDoc(node)
      val decoded: Option[A] =
        Document.Decoder.fromSchema(ks.schema).decode(documentRepr).toOption
      decoded.map { value =>
        Hints.Binding(ks.hintKey, value)
      }
    }

    def toHint(tr: traits.Trait): Option[Hint] = {
      val id = tr.toShapeId()
      val node = tr.toNode()
      hintsMap.get(id).flatMap(toHintAux(_, node))
    }

    val visitor =
      new CompileVisitor(model, schemaMap, endpointMap, serviceMap, toHint(_))
    model.shapes().iterator().asScala.foreach(_.accept(visitor))
    new DynamicModel(serviceMap.toMap, schemaMap.toMap)
  }

  class CompileVisitor(
      model: Model,
      schemaMap: MMap[ShapeId, Schema[DynData]],
      endpointMap: MMap[ShapeId, DynamicEndpoint],
      serviceMap: MMap[ShapeId, DynamicService],
      toHint: traits.Trait => Option[Hint]
  ) extends ShapeVisitor.Default[Unit] {

    def allHints(shape: Shape): Seq[Hint] = {
      shape
        .getAllTraits()
        .asScala
        .values
        .map(toHint)
        .collect { case Some(h) =>
          h
        }
        .toSeq
    }

    def allHints(shapeId: ShapeId): Seq[Hint] =
      allHints(model.expectShape(shapeId))

    def update[A](shape: Shape, schema: Schema[A]): Unit = {
      schemaMap += (shape.getId -> schema
        .withHints(allHints(shape): _*)
        .withHints(Name(shape.getId.getName()))
        .asInstanceOf[Schema[DynData]])
    }

    def getDefault(shape: Shape): Unit = ()

    override def integerShape(shape: IntegerShape): Unit =
      update(shape, int)

    override def booleanShape(shape: BooleanShape): Unit =
      update(shape, boolean)

    override def stringShape(shape: StringShape): Unit =
      update(shape, string)

    override def listShape(shape: ListShape): Unit =
      update(shape, list(suspend(schemaMap(shape.getMember.getTarget))))

    override def setShape(shape: SetShape): Unit =
      update(shape, set(suspend(schemaMap(shape.getMember.getTarget))))

    override def operationShape(shape: OperationShape): Unit = {
      def maybeSchema(maybeShapeId: Optional[ShapeId]): Schema[DynData] =
        suspend(
          maybeShapeId
            .map[Schema[DynData]](id => schemaMap(id))
            .orElseGet(() => unit.asInstanceOf[Schema[DynData]])
        )

      val ep = DynamicEndpoint(
        shape.getId.getNamespace(),
        shape.getId.getName(),
        maybeSchema(shape.getInput()),
        maybeSchema(shape.getOutput()),
        Hints(allHints(shape): _*)
      )
      endpointMap += shape.getId() -> ep
    }

    override def serviceShape(shape: ServiceShape): Unit = {
      val getEndpoints = () =>
        shape.getOperations().asScala.map(opId => endpointMap(opId)).toList
      val service = DynamicService(
        shape.getId().getNamespace(),
        shape.getId().getName(),
        shape.getVersion(),
        getEndpoints,
        Hints(allHints(shape): _*)
      )
      serviceMap += shape.getId() -> service
    }

    // Creates a dynamic structure array, unpacking options
    // when needed
    private final def dynStruct(fields: Vector[Any]): DynStruct = {
      val array = Array.ofDim[Any](fields.size)
      var i = 0
      fields.foreach {
        case None        => i += 1 // leaving value to null
        case Some(value) => (array(i) = value); i += 1
        case other       => (array(i) = other); i += 1
      }
      array
    }

    override def structureShape(shape: StructureShape): Unit =
      update(
        shape, {
          val shapeId = shape.getId()
          val namespace = shapeId.getNamespace()
          val shapeName = shapeId.getName()
          val members = shape.getAllMembers().asScala
          val fields =
            members.zipWithIndex.map { case ((label, mShape), index) =>
              val memberId = ShapeId.fromParts(namespace, shapeName, label)
              val memberHints = allHints(memberId)
              val memberSchema =
                suspend(schemaMap(mShape.getTarget()))
                  .withHints(memberHints: _*)
              if (mShape.getTrait(classOf[RequiredTrait]).isPresent()) {
                memberSchema.required[DynStruct](label, _(index))
              } else {
                memberSchema
                  .optional[DynStruct](label, arr => Option(arr(index)))
              }
            }.toVector
          genericStruct(fields)(dynStruct)
        }
      )
  }

}
