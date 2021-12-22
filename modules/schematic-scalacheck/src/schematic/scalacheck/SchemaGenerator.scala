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

package schematic
package scalacheck

import org.scalacheck.Gen
import org.scalacheck.Gen.const

object SchemaGenerator {
  type DefaultMetamodel[x[_]] = struct.Schematic[x]
    with union.Schematic[x]
    with schematic.CollectionSchematic[x]

}

/**
  * Contains logic to generate random schemas which work on an unsafe data
  * representation
  */
abstract class SchemaGenerator[
    S[x[_]] <: SchemaGenerator.DefaultMetamodel[x]
](maxWidth: Int)
    extends DynSchemaArity[S] {

  type DynSchema = Schema[S, DynData]
  type DynFieldSchema = StructureField[S, DynStruct, DynData]
  type DynAltSchema = OneOf[S, DynAlt, DynData]

  def primitives: Vector[DynSchema]

  def inductive(recurse: Gen[DynSchema]): Vector[Gen[DynSchema]] = {
    val fields: Gen[Vector[DynFieldSchema]] = for {
      numFields <- Gen.chooseNum(1, maxWidth)
      fields <- Gen.listOfN(numFields, genField(recurse)).map(_.toVector)
    } yield {
      distinctBy(fields)(_.label)
    }

    val genStruct: Gen[DynSchema] = for {
      fs <- fields.map(_.toVector)
    } yield dynStruct(fs)

    val numerousFields: Gen[Vector[DynFieldSchema]] = for {
      fields <- Gen.listOfN(30, genField(recurse)).map(_.toVector)
    } yield {
      distinctBy(fields)(_.label)
    }

    val genBigStruct = for {
      fs <- numerousFields
    } yield dynStruct(fs)

    val genUnion = for {
      numAlts <- Gen.chooseNum(1, maxWidth)
      firstAlt <- genAlt(recurse)
      alts <- Gen
        .listOfN(numAlts, genAlt(recurse))
        .map(_.toVector)
        .map(distinctBy(_)(_.label).toVector)
        .map(_.filterNot(_.label == firstAlt.label))
    } yield {
      new union.Schema[S, DynAlt](
        firstAlt,
        alts,
        { case (key: String, value) =>
          (alts.+:(firstAlt)).find(_.label == key).get.apply(value)
        }
      )
    }

    Vector(
      recurse.map(new list.Schema(_)),
      recurse.map(new set.Schema(_)),
      Gen.zip(recurse, recurse).map { case (k, v) => new map.Schema(k, v) },
      genStruct,
      genBigStruct,
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
        RequiredField(label, instance, (_: DynStruct).apply(label))
      else
        OptionalField[S, DynStruct, Any](
          label,
          instance,
          _.get(label)
        ).asInstanceOf[DynFieldSchema] // scalafix:ok
    }

  protected final def genAlt(recurse: Gen[DynSchema]): Gen[DynAltSchema] =
    for {
      key <- Gen.identifier.map(_.take(8))
      next <- recurse
    } yield OneOf(key, next, (d: DynData) => (key, d))

}
