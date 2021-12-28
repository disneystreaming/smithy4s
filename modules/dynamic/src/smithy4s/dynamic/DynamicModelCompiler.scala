package smithy4s
package dynamic

// import software.amazon.smithy.model.Model
import smithy4s.dynamic.model.{ShapeId => SID, _}
import scala.collection.mutable.{Map => MMap}
import schematic.OneOf
import schematic.StructureField
import smithy4s.syntax._

object Compiler {

  type DynSchema = Schema[DynData]
  type DynFieldSchema = StructureField[Schematic, DynStruct, DynData]
  type DynAltSchema = OneOf[Schematic, DynAlt, DynData]

  object ValidSID {
    def apply(sid: SID): Option[ShapeId] = {
      val segments = sid.value.split('#')
      if (segments.length == 2) {
        Some(ShapeId(segments(0), segments(1)))
      } else None
    }

    def unapply(sid: SID): Option[ShapeId] = apply(sid)
  }

  def toSID(shapeId: ShapeId): SID = SID(shapeId.namespace + "#" + shapeId.name)

  def compile(model: Model, knownHints: KeyedSchema[_]*): DynamicModel = {
    val schemaMap = MMap.empty[ShapeId, Schema[DynData]]
    val endpointMap = MMap.empty[ShapeId, DynamicEndpoint]
    val serviceMap = MMap.empty[ShapeId, DynamicService]

    val hintsMap: Map[ShapeId, KeyedSchema[_]] = knownHints.map { ks =>
      val shapeId: ShapeId =
        ShapeId.fromParts(ks.hintKey.namespace, ks.hintKey.name)
      shapeId -> ks
    }.toMap

    def toHintAux[A](
        ks: KeyedSchema[A],
        documentRepr: Document
    ): Option[Hint] = {
      val decoded: Option[A] =
        Document.Decoder.fromSchema(ks.schema).decode(documentRepr).toOption
      decoded.map { value =>
        Hints.Binding(ks.hintKey, value)
      }
    }

    def toHint(id: ShapeId, tr: Document): Option[Hint] = {
      hintsMap.get(id).flatMap(toHintAux(_, tr))
    }

    val visitor =
      new CompileVisitor(
        model,
        schemaMap,
        endpointMap,
        serviceMap,
        toHint(_, _)
      )

    model.shapes.foreach {
      case (ValidSID(id), shape) => visitor(id, shape)
      case _                     => ()
    }
    new DynamicModel(serviceMap.toMap, schemaMap.toMap)
  }

  class CompileVisitor(
      model: Model,
      schemaMap: MMap[ShapeId, Schema[DynData]],
      endpointMap: MMap[ShapeId, DynamicEndpoint],
      serviceMap: MMap[ShapeId, DynamicService],
      toHint: (ShapeId, Document) => Option[Hint]
  ) extends ShapeVisitor.Default[Unit] {

    def schema(sid: SID): Schema[DynData] = schemaMap(ValidSID.unapply(sid).get)

    def allHints(traits: Option[Map[SID, Document]]): Seq[Hint] = {
      traits
        .getOrElse(Map.empty)
        .collect { case (ValidSID(k), v) =>
          toHint(k, v)
        }
        .collect { case Some(h) =>
          h
        }
        .toSeq
    }

    def update[A](
        shapeId: ShapeId,
        traits: Option[Map[SID, Document]],
        schema: Schema[A]
    ): Unit = {
      schemaMap += (shapeId -> schema
        .withHints(allHints(traits): _*)
        .withHints(Name(shapeId.name))
        .asInstanceOf[Schema[DynData]])
    }

    def default: Unit = ()

    override def integerShape(id: ShapeId, shape: IntegerShape): Unit =
      update(id, shape.traits, int)

    override def booleanShape(id: ShapeId, shape: BooleanShape): Unit =
      update(id, shape.traits, boolean)

    override def stringShape(id: ShapeId, shape: StringShape): Unit =
      update(id, shape.traits, string)

    override def listShape(id: ShapeId, shape: ListShape): Unit =
      update(id, shape.traits, list(suspend(schema(shape.member.target))))

    override def setShape(id: ShapeId, shape: SetShape): Unit =
      update(id, shape.traits, set(suspend(schema(shape.member.target))))

    override def operationShape(id: ShapeId, shape: OperationShape): Unit = {
      def maybeSchema(maybeShapeId: Option[SID]): Schema[DynData] =
        suspend(
          maybeShapeId
            .flatMap(ValidSID(_))
            .map[Schema[DynData]](id => schemaMap(id))
            .getOrElse(unit.asInstanceOf[Schema[DynData]])
        )

      val ep = DynamicEndpoint(
        id.namespace,
        id.name,
        maybeSchema(shape.input.map(_.target)),
        maybeSchema(shape.output.map(_.target)),
        Hints(allHints(shape.traits): _*)
      )
      endpointMap += id -> ep
    }

    override def serviceShape(id: ShapeId, shape: ServiceShape): Unit = {
      val getEndpoints = () =>
        shape.operations.toList.map(_.target).collect { case ValidSID(opId) =>
          endpointMap(opId)
        }
      val service = DynamicService(
        id.namespace,
        id.name,
        shape.version.getOrElse(""),
        getEndpoints,
        Hints(allHints(shape.traits): _*)
      )
      serviceMap += id -> service
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

    override def structureShape(id: ShapeId, shape: StructureShape): Unit =
      update(
        id,
        shape.traits, {
          val members = shape.members.getOrElse(Map.empty)
          val fields =
            members.zipWithIndex.map { case ((label, mShape), index) =>
              val memberHints = allHints(mShape.traits)
              val memberSchema =
                suspend(schema(mShape.target))
                  .withHints(memberHints: _*)
              if (
                mShape.traits
                  .getOrElse(Map.empty)
                  .contains(SID("smithy.api#required"))
              ) {
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
