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

import munit._
import java.util.concurrent.atomic.AtomicInteger
import smithy4s.schema.SchemaVisitor
import smithy4s.schema._
import software.amazon.smithy.model.{Model => SModel}

class DynamicStabilitySpec extends FunSuite {

  val modelHeader = """|$version: "2.0"
                       |
                       |namespace foo
                       |""".stripMargin

  val header: String = "dynamic schemas are stable with respect to caching: "

  test(header + "primitive") {
    testSchema {
      "integer Foo"
    }
  }

  test(header + "collection") {
    testSchema {
      """|list Foo {
         |  member: Integer
         |}
         |""".stripMargin
    }
  }

  test(header + "enumeration") {
    testSchema {
      """|enum Foo {
         |  BAR = "BAR"
         |  BAZ = "BAZ"
         |}
         |""".stripMargin
    }
  }

  test(header + "map") {
    testSchema {
      """|map Foo {
         |  key: String
         |  value: String
         |}
         |""".stripMargin
    }
  }

  test(header + "struct") {
    testSchema {
      """|structure Foo {
         |  @required
         |  x: Integer
         |  y: String
         |}
         |""".stripMargin
    }
  }

  test(header + "union") {
    testSchema {
      """|union Foo {
         |  x: Integer
         |  y: String
         |}
         |""".stripMargin
    }
  }

  test(header + "recursive") {
    testSchema {
      """|structure Foo {
         |  foo: Foo
         |}
         |""".stripMargin
    }
  }

  def testSchema(modelString: String): Unit = {
    val fullModelString = modelHeader + "\n" + modelString

    val model = SModel
      .assembler()
      .addUnparsedModel("foo.smithy", fullModelString)
      .assemble()
      .unwrap()

    def parseAndLoad() = DynamicSchemaIndex
      .loadModel(model)
      .getOrElse(sys.error("Couldn't load model"))

    // We are testing that loading a schema several times and running
    // it through a cached visitor does actually hit the cache.
    val schema1 = parseAndLoad().getSchema(ShapeId("foo", "Foo"))
    val schema2 = parseAndLoad().getSchema(ShapeId("foo", "Foo"))
    val counter = new AtomicInteger(0)
    val visitor = new TestSchemaVisitor(counter)
    schema1.foreach(_.compile(visitor))
    val current = counter.get()
    schema2.foreach(_.compile(visitor))
    val newCurrent = counter.get()
    assertEquals(current, newCurrent)
    assertNotEquals(0, newCurrent)
  }

  type ConstUnit[A] = Unit
  class TestSchemaVisitor(counter: AtomicInteger)
      extends SchemaVisitor.Cached[ConstUnit] { self =>

    // Re-implementing compilation cache to count all the cache-miss
    protected val cache: CompilationCache[ConstUnit] =
      new CompilationCache[ConstUnit] {
        import scala.collection.mutable.Map
        private val store: Map[Any, Any] = Map.empty

        override def getOrElseUpdate[A](
            schema: Schema[A],
            fetch: Schema[A] => ConstUnit[A]
        ): ConstUnit[A] = {
          if (schema.isInstanceOf[Schema.LazySchema[_]]) { fetch(schema) }
          else
            store
              .getOrElseUpdate(
                schema,
                { counter.incrementAndGet(); fetch(schema) }
              )
              .asInstanceOf[ConstUnit[A]]
        }
      }

    private val visitedLazies: java.util.HashSet[ShapeId] =
      new java.util.HashSet[ShapeId]()

    def primitive[P](
        shapeId: ShapeId,
        hints: Hints,
        tag: Primitive[P]
    ): ConstUnit[P] = {}

    def collection[C[_], A](
        shapeId: ShapeId,
        hints: Hints,
        tag: CollectionTag[C],
        member: Schema[A]
    ): ConstUnit[C[A]] = { self(member) }

    def map[K, V](
        shapeId: ShapeId,
        hints: Hints,
        key: Schema[K],
        value: Schema[V]
    ): ConstUnit[Map[K, V]] = {
      self(key)
      self(value)
    }

    def enumeration[E](
        shapeId: ShapeId,
        hints: Hints,
        tag: EnumTag,
        values: List[EnumValue[E]],
        total: E => EnumValue[E]
    ): ConstUnit[E] = {
      val _ = counter.incrementAndGet()
    }

    def struct[S](
        shapeId: ShapeId,
        hints: Hints,
        fields: Vector[Field[S, _]],
        make: IndexedSeq[Any] => S
    ): ConstUnit[S] = {
      fields.foreach { field =>
        self(field.instance)
      }
    }

    def union[U](
        shapeId: ShapeId,
        hints: Hints,
        alternatives: Vector[SchemaAlt[U, _]],
        dispatch: Alt.Dispatcher[Schema, U]
    ): ConstUnit[U] = {
      alternatives.foreach { alt =>
        self(alt.instance)
      }
    }

    def biject[A, B](
        schema: Schema[A],
        bijection: Bijection[A, B]
    ): ConstUnit[B] = {
      self(schema)
    }

    def refine[A, B](
        schema: Schema[A],
        refinement: Refinement[A, B]
    ): ConstUnit[B] = {
      self(schema)
    }

    def lazily[A](suspend: Lazy[Schema[A]]): ConstUnit[A] = {
      val underlying = suspend.value
      if (!visitedLazies.contains(underlying.shapeId)) {
        visitedLazies.add(underlying.shapeId)
        self(underlying)
      }
    }

    def nullable[A](schema: Schema[A]): ConstUnit[Option[A]] = {
      self(schema)
    }
  }

}
