/*
 *  Copyright 2021-2024 Disney Streaming
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
import smithy4s.schema.{Alt, EnumTag, EnumValue, Field}
import smithy4s.schema.ErrorSchema
import DynamicLambdas._
import smithy4s.schema.Schema

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
      traits: Map[IdRef, Document]
  ): Option[A] =
    traits
      .get(toIdRef(implicitly[ShapeTag[A]].id))
      .flatMap { document =>
        document.decode[A].toOption
      }

  private def toHint(id: ShapeId, tr: Document): Hint =
    Hints.Binding.DynamicBinding(id, tr)

  /**
     * @param knownHints hints supported by the caller.
     */
  protected[dynamic] def compile(
      model: Model
  ): DynamicSchemaIndex = {
    val schemaMap = MMap.empty[ShapeId, Eval[Schema[DynData]]]
    // val endpointMap = MMap.empty[ShapeId, Eval[DynamicEndpoint]]
    val serviceMap = MMap.empty[ShapeId, Eval[DynamicService]]

    val visitor =
      new CompileVisitor(
        model,
        schemaMap,
        // endpointMap,
        serviceMap
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
      model.metadata,
      serviceMap.toMap.fmap(_.value),
      schemaMap.toMap.fmap(_.value)
    )
  }

  private class CompileVisitor(
      model: Model,
      schemaMap: MMap[ShapeId, Eval[Schema[DynData]]],
      serviceMap: MMap[ShapeId, Eval[DynamicService]]
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

    private def memberSchema(member: MemberShape): Eval[Schema[DynData]] =
      schema(member.target).map(_.addMemberHints(allHints(member.traits)))

    private def allHints(traits: Map[IdRef, Document]): Hints = {
      val ignoredHints = List(IdRef("smithy.api#enumValue"))

      Hints.fromSeq {
        (traits -- ignoredHints).collect { case (ValidIdRef(k), v) =>
          toHint(k, v)
        }.toSeq
      }
    }

    private def update[A](
        shapeId: ShapeId,
        traits: Map[IdRef, Document],
        lSchema: Eval[Schema[A]]
    ): Unit = {
      schemaMap += (shapeId -> lSchema.map { sch =>
        sch
          .withId(shapeId)
          .addHints(allHints(traits))
          .asInstanceOf[Schema[DynData]]
      })
    }

    private def update[A](
        shapeId: ShapeId,
        traits: Map[IdRef, Document],
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

    override def enumShape(id: ShapeId, shape: EnumShape): Unit = {
      val values: List[EnumValue[Int]] =
        shape.members.toList
          // Introducing arbitrary ordering for reproducible rendering.
          // Needs to happen before zipWithIndex so that intValue is also predictable.
          .sortBy(_._1)
          .flatMap { case (k, m) =>
            getTrait[smithy.api.EnumValue](m.traits).toList.map {
              _.value match {
                case Document.DString(s) => (k, s, m.traits)
                case v =>
                  throw new IllegalArgumentException(
                    s"enum value $k has a non-string value: $v"
                  )
              }
            }
          }
          .zipWithIndex
          .map { case ((name, stringValue, traits), i) =>
            EnumValue(
              stringValue = stringValue,
              intValue = i,
              value = i,
              name = name,
              hints = allHints(traits)
            )
          }

      update(id, shape.traits, makeStringEnum(id, values, shape.traits))
    }

    private def makeStringEnum(
        id: ShapeId,
        values: List[EnumValue[Int]],
        traits: Map[IdRef, Document]
    ) = {
      if (traits.contains(IdRef("alloy#openEnum"))) {
        // the runtime representation of normal enums is Int, but for open enums it's String to support arbitrary unknown values.
        val mappedValues = values.map(_.map(_.asLeft[String]))

        enumeration[Either[Int, String]](
          _.fold(
            mappedValues,
            unknownValueString =>
              EnumValue(
                stringValue = unknownValueString,
                intValue = -1,
                value = unknownValueString.asRight[Int],
                name = "$Unknown",
                hints = Hints.empty
              )
          ),
          EnumTag.OpenStringEnum(_.asRight[Int]),
          mappedValues
        )
      } else
        stringEnumeration(values, values)

    }

    override def intEnumShape(id: ShapeId, shape: IntEnumShape): Unit = {
      val values: Map[Int, EnumValue[Int]] = shape.members.toList
        .flatMap { case (k, m) =>
          getTrait[smithy.api.EnumValue](m.traits).toList.map {
            _.value match {
              case Document.DNumber(num) =>
                // toInt is safe because Smithy validates the model at loading time
                (k, num.toInt, m.traits)
              case v =>
                throw new IllegalArgumentException(
                  s"intEnum value $k has a non-numeric value: $v"
                )
            }
          }
        }
        .map { case (name, intValue, traits) =>
          intValue -> EnumValue(
            stringValue = name,
            intValue = intValue,
            value = intValue,
            name = name,
            hints = allHints(traits)
          )
        }
        .toMap

      val valueList = values.map(_._2).toList.sortBy(_.intValue)

      if (shape.traits.contains(IdRef("alloy#openEnum"))) {
        val theEnum = enumeration[Int](
          v =>
            values.getOrElse(
              v,
              EnumValue(
                stringValue = v.toString,
                intValue = v,
                value = v,
                name = "$Unknown",
                hints = Hints.empty
              )
            ),
          EnumTag.OpenIntEnum(identity),
          valueList
        )

        update(id, shape.traits, theEnum)
      } else {
        update(
          id,
          shape.traits,
          intEnumeration(values, valueList)
        )
      }
    }

    override def booleanShape(id: ShapeId, shape: BooleanShape): Unit =
      update(id, shape.traits, boolean)

    override def documentShape(id: ShapeId, shape: DocumentShape): Unit =
      update(id, shape.traits, document)

    override def stringShape(id: ShapeId, shape: StringShape): Unit = {
      val maybeUuid = getTrait[alloy.UuidFormat](shape.traits)
      val maybeEnum = getTrait[smithy.api.Enum](shape.traits)
      (maybeUuid, maybeEnum) match {
        case (Some(_), _) => update(id, shape.traits, uuid)
        case (None, Some(e)) => {
          val values = e.value.zipWithIndex.map {
            case (enumDefinition, intValue) =>
              val value = enumDefinition.value.value
              EnumValue(
                stringValue = value,
                intValue = intValue,
                // Using the intValue as a runtime value
                value = intValue,
                name = enumDefinition.name
                  .map(_.value)
                  .getOrElse(value.toUpperCase()),
                hints = Hints.empty
              )
          }
          update(
            id,
            shape.traits,
            makeStringEnum(id, values, shape.traits)
          )
        }
        case _ => update(id, shape.traits, string)
      }
    }

    override def listShape(id: ShapeId, shape: ListShape): Unit = {
      if (shape.traits.contains(IdRef("smithy.api#sparse"))) {
        update(id, shape.traits, memberSchema(shape.member).map(sparseList))
      } else {
        update(id, shape.traits, memberSchema(shape.member).map(list))
      }
    }

    override def setShape(id: ShapeId, shape: SetShape): Unit =
      update(id, shape.traits, schema(shape.member.target).map(s => set(s)))

    override def mapShape(id: ShapeId, shape: MapShape): Unit =
      update(
        id,
        shape.traits,
        for {
          k <- memberSchema(shape.key)
          v <- memberSchema(shape.value)
        } yield {
          if (shape.traits.contains(IdRef("smithy.api#sparse"))) {
            sparseMap(k, v).asInstanceOf[Schema[Map[Any, Any]]]
          } else map(k, v)
        }
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
      ): Eval[smithy4s.schema.Alt[DynAlt, DynData]] =
        getSchemaFromId(shapeId).map { schema =>
          Alt[DynAlt, DynData](
            shapeId.name,
            schema,
            (index, _: DynData),
            { case (`index`, dynData) => dynData }
          )
        }

      val input = shape.input.map(_.target)
      val output = shape.output.map(_.target)

      val errorId = id.withName(id.name + "Error")
      val allOperationErrors = (serviceErrors ++ shape.errors).toNel

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
              { case (index, _) => index }
            )
          )
      }
      val errorschemaLazy = errorUnionLazy.map(
        _.map(DynamicErrorSchema(_).asInstanceOf[ErrorSchema[Any]])
      )

      for {
        inputSchema <- getSchema(input).map(_.addHints(InputOutput.Input.widen))
        errorschema <- errorschemaLazy
        outputSchema <- getSchema(output).map(
          _.addHints(InputOutput.Output.widen)
        )
      } yield {
        DynamicEndpoint(
          Schema
            .operation(id)
            .withInput(inputSchema)
            .withOutput(outputSchema)
            .withErrorOption(errorschema)
            .withHints(allHints(shape.traits))
        )
      }
    }

    override def serviceShape(id: ShapeId, shape: ServiceShape): Unit = {
      def resourceOperations(resource: MemberShape): List[MemberShape] = {
        model.shapes
          .get(resource.target)
          .map(resource.target -> _)
          .collect { case (ValidIdRef(_), Shape.ResourceCase(resource)) =>
            resource
          }
          .toList
          .flatMap { resource =>
            val lifecycle = List(
              resource.create,
              resource.put,
              resource.read,
              resource.update,
              resource.delete,
              resource.list
            ).flatten
            val operations = resource.operations
            val recursive =
              resource.resources.flatMap(resourceOperations)
            List.concat(lifecycle, operations, recursive)
          }

      }
      val serviceErrors: List[MemberShape] = shape.errors

      val operations = List
        .concat(
          shape.operations,
          shape.resources.flatMap(resourceOperations)
        )
      val lEndpoints =
        operations
          .flatMap(op => model.shapes.get(op.target).map(op.target -> _))
          .collect { case (ValidIdRef(id), Shape.OperationCase(op)) =>
            compileOperation(id, serviceErrors, op)
          }
          .sequence

      val service = lEndpoints.map { endpoints =>
        DynamicService(
          id,
          shape.version.getOrElse(""),
          endpoints.toVector,
          allHints(shape.traits)
        )
      }

      serviceMap += id -> service
    }

    override def structureShape(id: ShapeId, shape: StructureShape): Unit =
      update(
        id,
        shape.traits, {
          val lFields = {
            shape.members.toVector.zipWithIndex.traverse {
              case ((label, mShape), index) =>
                val lMemberSchema = schema(mShape.target)
                val lField =
                  if (
                    mShape.traits
                      .contains(IdRef("smithy.api#required"))
                  ) {
                    lMemberSchema.map(
                      _.required[DynStruct](label, Accessor(index))
                    )
                  } else {
                    lMemberSchema.map(
                      _.optional[DynStruct](label, OptionalAccessor(index))
                        .asInstanceOf[Field[DynStruct, DynData]]
                    )
                  }
                val memberHints = allHints(mShape.traits)
                lField.map(_.addHints(memberHints.all.toSeq: _*))
            }
          }
          if (isRecursive(id)) {
            Eval.later(recursive(struct(lFields.value)(Constructor)))
          } else lFields.map(fields => struct(fields)(Constructor))
        }
      )

    override def unionShape(id: ShapeId, shape: UnionShape): Unit = {
      update(
        id,
        shape.traits, {
          val lAlts =
            shape.members.toVector.zipWithIndex.traverse {
              case ((label, mShape), index) =>
                val memberHints = allHints(mShape.traits)
                schema(mShape.target)
                  .map(
                    _.oneOf[DynAlt](label, Injector(index))(Projector(index))
                  )
                  .map(_.addHints(memberHints))
            }
          if (isRecursive(id)) {
            Eval.later(recursive {
              val alts = lAlts.value
              union(alts)(Ordinal)
            })
          } else
            lAlts.map { alts =>
              union(alts)(Ordinal)
            }
        }
      )
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
      fromMembers(shape.members.values.toSet)

    override def unionShape(
        id: ShapeId,
        shape: UnionShape
    ): Set[ShapeId] =
      fromMembers(shape.members.values.toSet)

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

    override def enumShape(
        id: ShapeId,
        shape: EnumShape
    ): Set[ShapeId] =
      fromMembers(shape.members.values.toSet)

    override def intEnumShape(
        id: ShapeId,
        shape: IntEnumShape
    ): Set[ShapeId] =
      fromMembers(shape.members.values.toSet)
  }

}
