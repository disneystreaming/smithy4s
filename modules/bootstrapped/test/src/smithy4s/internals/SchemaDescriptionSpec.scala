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
      Schema.timestamp,
      Schema.document,
      Schema.uuid
    ).map(_.asInstanceOf[Schema[Any]])
  )

  def simple[A](s: Schema[A]): String = s.compile(SchemaDescription)
  def detailed[A](s: Schema[A]): String =
    s.compile(SchemaDescriptionDetailed)

  case class TestCase[A](schema: Schema[A], simple: String, detailed: String)

  val testCases = Seq(
    TestCase(SomeInt.schema, "Int", "Int"),
    TestCase(StringSet.schema, "Set", "Set[String]"),
    TestCase(StringList.schema, "List", "List[String]"),
    TestCase(StringMap.schema, "Map", "Map[String, String]"),
    TestCase(
      UntaggedUnion.schema,
      "Union",
      "union UntaggedUnion(three: structure Three(three: String) | four: structure Four(four: Int))"
    ),
    TestCase(FooEnum.schema, "Enumeration", "enum(\"Foo\")"),
    TestCase(
      IntList.schema,
      "Structure",
      "structure IntList(head: Int, tail: Option[Recursive[IntList]])"
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
