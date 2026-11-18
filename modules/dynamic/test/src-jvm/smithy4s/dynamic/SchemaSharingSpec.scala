/*
 *  Copyright 2021-2026 Disney Streaming
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

import smithy4s.ShapeId
import smithy4s.schema.Field
import smithy4s.schema.Schema

class SchemaSharingSpec() extends DummyIO.Suite {

  // S0 -> (S1, S1) -> (S2, S2) -> ... -> Sn: every structure is referenced
  // twice by its parent, so without sharing, computing S0 takes 2^n steps.
  private def diamondChain(depth: Int): String = {
    val structs = (0 until depth).map { i =>
      s"structure S$i { a: S${i + 1}, b: S${i + 1} }"
    }
    (List("namespace foo") ++ structs :+ s"string S$depth").mkString("\n")
  }

  private def fieldsOf(schema: Schema[_]): Vector[Field[_, _]] = schema match {
    case s: Schema.StructSchema[_]       => s.fields
    case Schema.OptionSchema(underlying) => fieldsOf(underlying)
    case other => fail(s"Expected a structure schema, got $other")
  }

  test("Schemas of shapes referenced from multiple places are shared") {
    Utils.compile(diamondChain(3)).map { index =>
      val s0 = index.getSchema(ShapeId("foo", "S0")).get
      val s1 = index.getSchema(ShapeId("foo", "S1")).get

      // Member schemas are copies (they carry member hints),
      // but the copies keep the original's fields.
      fieldsOf(s0).foreach { field =>
        assert(fieldsOf(field.schema) eq fieldsOf(s1), field.label)
      }
    }
  }

  test("Compilation of deeply shared shapes doesn't take exponential time") {
    // 2^40 steps would never finish without sharing.
    Utils.compile(diamondChain(40)).map { index =>
      assert(index.getSchema(ShapeId("foo", "S0")).isDefined)
    }
  }
}
