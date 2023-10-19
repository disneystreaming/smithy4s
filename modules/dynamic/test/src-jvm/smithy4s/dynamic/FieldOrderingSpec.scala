/*
 *  Copyright 2021-2023 Disney Streaming
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

package smithy4s.dynamic

import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.shapes.UnionShape
import software.amazon.smithy.model.Model
import smithy4s.schema.Schema
import org.scalacheck.Shrink

class FieldOrderingSpec extends munit.ScalaCheckSuite {

  property("Ordering of fields is retained") {
    val genStrings = Gen.listOfN(10, Gen.identifier).map(_.distinct)
    implicit def noShrink[A]: Shrink[A] = Shrink.shrinkAny
    forAll(genStrings) { (names: List[String]) =>
      val structBuilder = StructureShape.builder().id("foo#Foo")
      val unionBuilder = UnionShape.builder().id("foo#Bar")
      names.foreach { name =>
        structBuilder.addMember(name, ShapeId.from("smithy.api#Integer"))
        unionBuilder.addMember(name, ShapeId.from("smithy.api#Integer"))
      }
      val struct = structBuilder.build()
      val union = unionBuilder.build()
      val model = Model.builder().addShapes(struct, union).build()
      val schemaIndex = DynamicSchemaIndex.loadModel(model)

      for {
        id <- List("Foo", "Bar")
      } {
        val schema = schemaIndex
          .getSchema(smithy4s.ShapeId("foo", id))
          .getOrElse(fail(s"Error: $id shape missing"))

        schema match {
          case Schema.StructSchema(_, _, fields, _) =>
            val fieldNames = fields.map(_.label).toList
            assertEquals(fieldNames, names)
          case Schema.UnionSchema(_, _, alts, _) =>
            val altNames = alts.map(_.label).toList
            assertEquals(altNames, names)
          case unexpected => fail("Unexpected schema: " + unexpected)
        }
      }
    }
  }

}
