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

package smithy4s
package dynamic
package internals

import smithy4s.dynamic.model._
import scala.collection.mutable.{Map => MMap}
import smithy4s.schema.Schema._
import smithy4s.internals.InputOutput
import cats.Eval
import cats.syntax.all._
import smithy4s.schema.EnumValue
import smithy4s.schema.Alt

private[dynamic] object Compiler {

  private object ValidIdRef {
    def apply(idRef: IdRef): Option[ShapeId] = {
      val segments = idRef.value.split('#')
      if (segments.length == 2) {
        Some(ShapeId(segments(0), segments(1)))
      } else None
    }

    def unapply(idRef: IdRef): Option[ShapeId] = apply(idRef)
  }

  private def toIdRef(shapeId: ShapeId): IdRef = IdRef(
    shapeId.namespace + "#" + shapeId.name
  )

  private def getTrait[A: ShapeTag: Document.Decoder](
      traits: Option[Map[IdRef, Document]]
  ): Option[A] =
    traits.flatMap(dynamicTraits =>
      dynamicTraits.get(toIdRef(implicitly[ShapeTag[A]].id)).flatMap {
        document =>
          document.decode[A].toOption
      }
    )

  /**
     * @param knownHints hints supported by the caller.
     */
  protected[dynamic] def compile(
      model: Model,
      knownHints: SchemaIndex
  ): DynamicSchemaIndex = {
    val schemaMap = MMap.empty[ShapeId, Eval[Schema[DynData]]]
    // val endpointMap = MMap.empty[ShapeId, Eval[DynamicEndpoint]]
    val serviceMap = MMap.empty[ShapeId, Eval[DynamicService]]

    val hintsMap: Map[ShapeId, SchemaIndex.Binding[_]] = {
      def binding[A](
          tag: ShapeTag[A]
      ): Option[(ShapeId, SchemaIndex.Binding[A])] =
        knownHints.get(tag).map(s => tag.id -> SchemaIndex.Binding(tag, s))
      type B = (ShapeId, SchemaIndex.Binding[_])
      knownHints.tags
        .flatMap(binding(_): Option[B])
        .toMap
    }

    def toHintAux[A](
        ks: SchemaIndex.Binding[A],
        documentRepr: Document
    ): Option[Hint] = {
      val decoded: Option[A] =
        Document.Decoder.fromSchema(ks.schema).decode(documentRepr).toOption
      decoded.map { value =>
        Hints.Binding(ks.tag, value)
      }
    }

    def toHint(id: ShapeId, tr: Document): Option[Hint] =
      hintsMap.get(id).flatMap(toHintAux(_, tr))

    val visitor =
      new CompileVisitor(
        model,
        schemaMap,
        // endpointMap,
        serviceMap,
        toHint(_, _)
      )

    // Loosely inspired by
    // https://awslabs.github.io/smithy/1.0/spec/core/prelude-model.html#prelude-shapes
    List(
      "String" -> Shape.StringCase(StringShape()),
      "Blob" -> Shape.BlobCase(BlobShape()),
      "BigInteger" -> Shape.BigIntegerCase(BigIntegerShape()),
      "BigDecimal" -> Shape.BigDecimalCase(BigDecimalShape()),
      "Timestamp" -> Shape.TimestampCase(TimestampShape()),
      "Document" -> Shape.DocumentCase(DocumentShape()),
      "Boolean" -> Shape.BooleanCase(BooleanShape()),
      "Byte" -> Shape.ByteCase(ByteShape()),
      "Short" -> Shape.ShortCase(ShortShape()),
      "Integer" -> Shape.IntegerCase(IntegerShape()),
      "Long" -> Shape.LongCase(LongShape()),
      "Float" -> Shape.FloatCase(FloatShape()),
      "Double" -> Shape.DoubleCase(DoubleShape()),
      "Unit" -> Shape.StructureCase(StructureShape()),
      "PrimitiveBoolean" -> Shape.BooleanCase(BooleanShape()),
      "PrimitiveByte" -> Shape.ByteCase(ByteShape()),
      "PrimitiveShort" -> Shape.ShortCase(ShortShape()),
      "PrimitiveInteger" -> Shape.IntegerCase(IntegerShape()),
      "PrimitiveLong" -> Shape.LongCase(LongShape()),
      "PrimitiveFloat" -> Shape.FloatCase(FloatShape()),
      "PrimitiveDouble" -> Shape.DoubleCase(DoubleShape())
    ).foreach { case (id, shape) => visitor(ShapeId("smithy.api", id), shape) }

    model.shapes.foreach {
      case (ValidIdRef(id), shape) => visitor(id, shape)
      case _                       => ()
    }
    new DynamicSchemaIndexImpl(
      serviceMap.toMap.fmap(_.value),
      schemaMap.toMap.fmap(_.value)
    )
  }

  private class CompileVisitor(
      model: Model,
      schemaMap: MMap[ShapeId, Eval[Schema[DynData]]],
      serviceMap: MMap[ShapeId, Eval[DynamicService]],
      toHint: (ShapeId, Document) => Option[Hint]
  ) extends ShapeVisitor.Default[Unit] {

    private val closureMap: Map[ShapeId, Set[ShapeId]] = model.shapes.collect {
      case (ValidIdRef(shapeId), shape) =>
        shapeId -> ClosureVisitor(shapeId, shape)
    }

    private def isRecursive(
        id: ShapeId,
        visited: Set[ShapeId] = Set.empty
    ): Boolean = {
      def transitiveClosure(
          _id: ShapeId,
          visited: Set[ShapeId]
      ): Set[ShapeId] = {
        val newVisited = visited + _id
        val neighbours = closureMap.getOrElse(_id, Set.empty)
        val nonVisitedNeighbours = neighbours.filterNot(newVisited)
        val neighbourClosures =
          nonVisitedNeighbours.flatMap(transitiveClosure(_, newVisited))
        neighbours ++ neighbourClosures
      }
      val closure = transitiveClosure(id, Set.empty)
      // A type is recursive if it's referenced in its own closure
      closure.contains(id)
    }

    private def schema(idRef: IdRef): Eval[Schema[DynData]] = Eval.defer {
      schemaMap(
        ValidIdRef.unapply(idRef).get
      )
    }

    private def allHints(traits: Option[Map[IdRef, Document]]): Seq[Hint] = {
      traits
        .getOrElse(Map.empty)
        .collect { case (ValidIdRef(k), v) =>
          toHint(k, v)
        }
        .collect { case Some(h) =>
          h
        }
        .toSeq
    }

    private def update[A](
        shapeId: ShapeId,
        traits: Option[Map[IdRef, Document]],
        lSchema: Eval[Schema[A]]
    ): Unit = {
      schemaMap += (shapeId -> lSchema.map { sch =>
        sch
          .withId(shapeId)
          .addHints(allHints(traits): _*)
          .asInstanceOf[Schema[DynData]]
      })
    }

    def update[A](
        shapeId: ShapeId,
        traits: Option[Map[IdRef, Document]],
        schema: Schema[A]
    ): Unit = update(shapeId, traits, Eval.now(schema))

    def default: Unit = ()

    override def integerShape(id: ShapeId, shape: IntegerShape): Unit =
      update(id, shape.traits, int)

    override def floatShape(id: ShapeId, shape: FloatShape): Unit =
      update(id, shape.traits, float)

    override def longShape(id: ShapeId, shape: LongShape): Unit =
      update(id, shape.traits, long)

    override def doubleShape(id: ShapeId, shape: DoubleShape): Unit =
      update(id, shape.traits, double)

    override def shortShape(id: ShapeId, shape: ShortShape): Unit =
      update(id, shape.traits, short)

    override def bigIntegerShape(id: ShapeId, shape: BigIntegerShape): Unit =
      update(id, shape.traits, bigint)

    override def bigDecimalShape(id: ShapeId, shape: BigDecimalShape): Unit =
      update(id, shape.traits, bigdecimal)

    override def byteShape(id: ShapeId, shape: ByteShape): Unit =
      update(id, shape.traits, byte)

    override def timestampShape(id: ShapeId, shape: TimestampShape): Unit =
      update(id, shape.traits, timestamp)

    override def blobShape(id: ShapeId, shape: BlobShape): Unit =
      update(id, shape.traits, bytes)

    override def booleanShape(id: ShapeId, shape: BooleanShape): Unit =
      update(id, shape.traits, boolean)

    override def documentShape(id: ShapeId, shape: DocumentShape): Unit =
      update(id, shape.traits, document)

    override def stringShape(id: ShapeId, shape: StringShape): Unit = {
      val maybeUuid = getTrait[smithy4s.api.UuidFormat](shape.traits)
      val maybeEnum = getTrait[smithy.api.Enum](shape.traits)
      (maybeUuid, maybeEnum) match {
        case (Some(_), _) => update(id, shape.traits, uuid)
        case (None, Some(e)) => {
          // Using the ordinal as a runtime value
          val values = e.value.map(_.value.value).zipWithIndex.map {
            case (stringValue, ordinal) =>
              EnumValue(
                stringValue,
                ordinal,
                ordinal,
                Hints.empty
              )
          }
          val fromOrdinal = values(_: Int)
          update(id, shape.traits, enumeration(fromOrdinal, values))
        }
        case _ => update(id, shape.traits, string)
      }
    }

    override def listShape(id: ShapeId, shape: ListShape): Unit =
      update(id, shape.traits, schema(shape.member.target).map(s => list(s)))

    override def setShape(id: ShapeId, shape: SetShape): Unit =
      update(id, shape.traits, schema(shape.member.target).map(s => set(s)))

    override def mapShape(id: ShapeId, shape: MapShape): Unit =
      update(
        id,
        shape.traits,
        for {
          k <- schema(shape.key.target)
          v <- schema(shape.value.target)
        } yield map(k, v)
      )

    override def operationShape(id: ShapeId, x: OperationShape): Unit = {}

    def compileOperation(
        id: ShapeId,
        serviceErrors: List[MemberShape],
        shape: OperationShape
    ): Eval[DynamicEndpoint] = {
      def getSchemaFromId(shapeId: ShapeId): Eval[Schema[DynData]] =
        Eval.defer(schemaMap(shapeId))

      def getSchema(maybeShapeId: Option[IdRef]): Eval[Schema[DynData]] =
        maybeShapeId
          .flatMap(ValidIdRef(_))
          .map[Eval[Schema[DynData]]](getSchemaFromId)
          .getOrElse(Eval.now(unit.asInstanceOf[Schema[DynData]]))

      def errorAlt(
          index: Int,
          shapeId: ShapeId
      ): Eval[smithy4s.schema.SchemaAlt[DynAlt, DynData]] =
        getSchemaFromId(shapeId).map { schema =>
          Alt[Schema, DynAlt, DynData](
            shapeId.name,
            schema,
            (index, _: DynData),
            Hints.empty
          )
        }

      val input = shape.input.map(_.target)
      val output = shape.output.map(_.target)

      val errorId = id.copy(name = id.name + "Error")
      val allOperationErrors = (serviceErrors ++ shape.errors.combineAll).toNel

      val errorUnionLazy = allOperationErrors.traverse { err =>
        err
          .collect { case MemberShape(ValidIdRef(id), _) => id }
          .zipWithIndex
          .traverse { case (id, idx) => errorAlt(idx, id) }
          .map(alts =>
            Schema.UnionSchema[DynAlt](
              errorId,
              Hints.empty,
              alts.toVector,
              { case (index, data) =>
                alts(index).apply(data)
              }
            )
          )
      }
      val errorableLazy = errorUnionLazy.map(
        _.map(DynamicErrorable(_).asInstanceOf[Errorable[Any]])
      )

      for {
        inputSchema <- getSchema(input).map(_.addHints(InputOutput.Input))
        errorable <- errorableLazy
        outputSchema <- getSchema(output).map(_.addHints(InputOutput.Output)),
      } yield {
        DynamicEndpoint(
          id,
          inputSchema,
          outputSchema,
          errorable,
          Hints(allHints(shape.traits): _*)
        )
      }
    }

    override def serviceShape(id: ShapeId, shape: ServiceShape): Unit = {
      val serviceErrors: List[MemberShape] =
        shape.errors.toList.flatten
      val lEndpoints =
        shape.operations.toList
          .flatMap(_.map(_.target))
          .flatMap(id => model.shapes.get(id).map(id -> _))
          .collect { case (ValidIdRef(id), Shape.OperationCase(op)) =>
            compileOperation(id, serviceErrors, op)
          }
          .sequence

      val service = lEndpoints.map { endpoints =>
        DynamicService(
          id,
          shape.version.getOrElse(""),
          endpoints,
          Hints(allHints(shape.traits): _*)
        )
      }

      serviceMap += id -> service
    }

    // Creates a dynamic structure array, unpacking options
    // when needed
    private final def dynStruct(fields: IndexedSeq[Any]): DynStruct = {
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
          val lFields = {
            members.zipWithIndex
              .map { case ((label, mShape), index) =>
                val memberHints = allHints(mShape.traits)
                val lMemberSchema =
                  schema(mShape.target).map(_.addHints(memberHints: _*))
                if (
                  mShape.traits
                    .getOrElse(Map.empty)
                    .contains(IdRef("smithy.api#required"))
                ) {
                  lMemberSchema.map(_.required[DynStruct](label, _(index)))
                } else {
                  lMemberSchema.map(
                    _.optional[DynStruct](label, arr => Option(arr(index)))
                  )
                }
              }
              .toVector
              .sequence
          }
          if (isRecursive(id)) {
            Eval.later(recursive(struct(lFields.value)(dynStruct)))
          } else lFields.map(fields => struct(fields)(dynStruct))
        }
      )

    override def unionShape(id: ShapeId, shape: UnionShape): Unit = {
      shape.members.filter(_.nonEmpty).foreach { members =>
        update(
          id,
          shape.traits, {
            val lAlts =
              members.zipWithIndex
                .map { case ((label, mShape), index) =>
                  val memberHints = allHints(mShape.traits)
                  schema(mShape.target)
                    .map(_.addHints(memberHints: _*))
                    .map(_.oneOf[DynAlt](label, (data: Any) => (index, data)))
                }
                .toVector
                .sequence
            if (isRecursive(id)) {
              Eval.later(recursive {
                val alts = lAlts.value
                union(alts) { case (index, data) =>
                  alts(index).apply(data)
                }
              })
            } else
              lAlts.map { alts =>
                union(alts) { case (index, data) =>
                  alts(index).apply(data)
                }
              }
          }
        )
      }
    }
  }

  // A visitor allowing to gather the "closure" of all shapes
  private object ClosureVisitor extends ShapeVisitor.Default[Set[ShapeId]] {
    def default: Set[ShapeId] = Set.empty

    def fromMembers(it: Iterable[MemberShape]): Set[ShapeId] =
      it.map(_.target).collect { case ValidIdRef(id) => id }.toSet

    override def structureShape(
        id: ShapeId,
        shape: StructureShape
    ): Set[ShapeId] =
      fromMembers(shape.members.foldMap(_.values.toSet))

    override def unionShape(
        id: ShapeId,
        shape: UnionShape
    ): Set[ShapeId] =
      fromMembers(shape.members.foldMap(_.values.toSet))

    override def listShape(
        id: ShapeId,
        shape: ListShape
    ): Set[ShapeId] =
      fromMembers(Set(shape.member))

    override def setShape(
        id: ShapeId,
        shape: SetShape
    ): Set[ShapeId] =
      fromMembers(Set(shape.member))

    override def mapShape(
        id: ShapeId,
        shape: MapShape
    ): Set[ShapeId] =
      fromMembers(Set(shape.key, shape.value))
  }

}
