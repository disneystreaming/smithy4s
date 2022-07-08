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
package internals

import smithy4s.example._
import munit._
import org.scalacheck.Gen
import org.scalacheck.Prop
import org.scalacheck.Prop._

class SchemaDescriptionSpec() extends FunSuite with ScalaCheckSuite { self =>
  val genPrim: Gen[Schema[Any]] = Gen.oneOf[Schema[Any]](
    Seq[Schema[_]](
      Schema.short,
      Schema.int,
      Schema.long,
      Schema.double,
      Schema.float,
      Schema.bigint,
      Schema.bigdecimal,
      Schema.string,
      Schema.boolean,
      Schema.byte,
      Schema.bytes,
      Schema.unit,
      Schema.timestamp,
      Schema.document,
      Schema.uuid
    ).map(_.asInstanceOf[Schema[Any]])
  )

  def simple[A](s: Schema[A]): String = s.compile(SchemaDescription)
  def detailed[A](s: Schema[A]): String =
    s.compile(SchemaDescriptionDetailed)(Set.empty)._2

  case class TestCase[A](schema: Schema[A], simple: String, detailed: String)

  val testCases = Seq(
    TestCase(SomeInt.schema, "Int", "Bijection{ Int }"),
    TestCase(SomeSet.schema, "Set", "Bijection{ Set[String] }"),
    TestCase(SomeList.schema, "List", "Bijection{ List[String] }"),
    TestCase(SomeMap.schema, "Map", "Bijection{ Map[String, String] }"),
    TestCase(
      UntaggedUnion.schema,
      "Union",
      "Union{ three: Bijection{ Structure Three{ three: String } }, four: Bijection{ Structure Four{ four: Int } } }"
    ),
    TestCase(
      BlobBody.schema,
      "Structure",
      "Structure BlobBody{ blob: Bytes }"
    ),
    TestCase(FooEnum.schema, "Enumeration", "Enumeration{ Foo }"),
    TestCase(
      IntList.schema,
      "Structure",
      "Structure IntList{ head: Int, tail: Recursive{ IntList } }"
    ) // recursive
  )

  testCases.foreach { tc =>
    test(s"simple ${tc.schema.shapeId}") {
      assertEquals(simple(tc.schema), tc.simple)
    }

    test(s"detailed ${tc.schema.shapeId}") {
      assertEquals(detailed(tc.schema), tc.detailed)
    }
  }

  property("Check all primitive") {
    forAll(genPrim) { (prim: Schema[Any]) =>
      Prop(self.simple(prim) == self.detailed(prim))
    }
  }
}
