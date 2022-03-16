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

package smithy4s
package scalacheck

import smithy4s.schema._
import smithy4s.schema.Schema._

import org.scalacheck.Gen
import org.scalacheck.Gen.const
import smithy.api.TimestampFormat

object SchemaGenerator {

  def genSchema(
      maxDepth: Int,
      maxWidth: Int
  ): Gen[Schema[DynData]] = {
    val generator = new SchemaGenerator(maxWidth)
    generator.gen(maxDepth)
  }

}

/**
  * Contains logic to generate random schemas which work on an unsafe data
  * representation
  */
class SchemaGenerator(maxWidth: Int) {

  type DynSchema = Schema[DynData]
  type DynFieldSchema = SchemaField[DynStruct, DynData]
  type DynAltSchema = SchemaAlt[DynAlt, DynData]

  def primitives: Vector[DynSchema] =
    Vector(
      boolean,
      bytes,
      byte,
      string,
      int,
      long,
      float,
      double,
      short,
      uuid,
      unit,
      document,
      timestamp,
      timestamp.addHints(TimestampFormat.DATE_TIME),
      timestamp.addHints(TimestampFormat.EPOCH_SECONDS),
      timestamp.addHints(TimestampFormat.HTTP_DATE)
    ).asInstanceOf[Vector[DynSchema]]

  def inductive(recurse: Gen[DynSchema]): Vector[Gen[DynSchema]] = {
    val fields: Gen[Vector[DynFieldSchema]] = for {
      numFields <- Gen.chooseNum(1, maxWidth)
      fields <- Gen.listOfN(numFields, genField(recurse)).map(_.toVector)
    } yield {
      distinctBy(fields)(_.label)
    }

    def makeStruct(fields: Vector[DynFieldSchema]): DynSchema = {
      Schema.struct
        .genericArity[DynStruct](fields: _*) { values =>
          dynStruct(fields.map(_.label).zip(values): _*)
        }
        .asInstanceOf[DynSchema]
    }

    val genStruct: Gen[DynSchema] = for {
      fs <- fields.map(_.toVector)
    } yield makeStruct(fs)

    val genUnion = for {
      numAlts <- Gen.chooseNum(1, maxWidth)
      alts <- Gen
        .listOfN(numAlts, genAlt(recurse))
        .map(_.toVector)
        .map(distinctBy(_)(_.label).toVector)
    } yield {
      union[DynAlt](alts: _*) { case (key: String, value) =>
        alts.find(_.label == key).get.apply(value)
      }
    }

    Vector(
      recurse.map(Schema.list(_)),
      recurse.map(Schema.set(_)),
      Gen.zip(recurse, recurse).map { case (k, v) => map(k, v) },
      genStruct,
      genUnion
    ).asInstanceOf[Vector[Gen[DynSchema]]]
  }

  final def gen(depth: Int): Gen[DynSchema] = {
    if (depth == 0) {
      val prims = primitives.map(Gen.const(_))
      if (prims.size >= 2) Gen.oneOf(prims(0), prims(1), prims.drop(2): _*)
      else prims(0)
    } else {
      val newDepthG = Gen.chooseNum(0, depth - 1)
      val recurse = newDepthG.flatMap(gen(_))
      val ind = inductive(recurse)
      if (ind.size >= 2) Gen.oneOf(ind(0), ind(1), ind.drop(2): _*)
      else ind(0)
    }
  }.asInstanceOf[Gen[DynSchema]] // scalafix:ok

  protected final def dynStruct(fields: (String, Any)*): DynStruct = {
    val unwrapped = fields.filterNot(_._2 == None).collect {
      case (label, Some(value)) => (label, value)
      case (label, other)       => (label, other)
    }
    Map(unwrapped: _*)
  }

  protected final def genField(
      recurse: Gen[DynSchema]
  ): Gen[DynFieldSchema] =
    for {
      label <- Gen.identifier.map(_.take(8))
      instance <- recurse
      required <- Gen.oneOf(true, false)
    } yield {
      if (required)
        Field.required(label, instance, (_: DynStruct).apply(label))
      else
        Field
          .optional(
            label,
            instance,
            (_: DynStruct).get(label)
          )
          .asInstanceOf[DynFieldSchema] // scalafix:ok
    }

  protected final def genAlt(recurse: Gen[DynSchema]): Gen[DynAltSchema] =
    for {
      key <- Gen.identifier.map(_.take(8))
      next <- recurse
    } yield Alt(key, next, (d: DynData) => (key, d), Hints.empty)

}
