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

package smithy4s.schema

import munit._
import smithy4s.Hints
import Schema._

class SchemaHashSpec() extends FunSuite {

  def checkSchema[A](schema: Schema[A])(implicit loc: Location): Unit = {
    def transform() =
      schema.transformHintsTransitively(_ ++ Hints(smithy.api.Deprecated()))
    val transformed1 = transform().schemaHash
    val transformed2 = transform().schemaHash
    assertNotEquals(transformed1, 1)
    assertEquals(transformed1, transformed2)
  }

  val header = "schema hash equal through hints transformation: "

  test(header + "primitive") {
    checkSchema(int)
  }

  test(header + "collection") {
    checkSchema(list(int))
  }

  test(header + "map") {
    checkSchema(map(string, int))
  }

  test(header + "enum") {
    sealed abstract class FooBar(val stringValue: String, val intValue: Int)
        extends smithy4s.Enumeration.Value {
      val name = stringValue
      val value = stringValue
      val hints = Hints.empty
    }
    case object Foo extends FooBar("foo", 0)
    case object Bar extends FooBar("bar", 1)
    val schema: Schema[FooBar] = enumeration[FooBar](List(Foo, Bar))
    checkSchema(schema)
  }

  test(header + "struct") {
    case class Foo(int: Int)
    val schema = struct(int.required[Foo]("int", _.int))(Foo(_))
    checkSchema(schema)
  }

  test(header + "union") {
    type Foo = Either[Int, String]
    val left = int.oneOf[Foo]("left", Left(_))
    val right = string.oneOf[Foo]("right", Right(_))
    val schema = union(left, right) {
      case Left(int)     => left(int)
      case Right(string) => right(string)
    }

    checkSchema(schema)
  }

  test(header + "lazy") {
    case class Foo(foo: Option[Foo])
    object Foo {
      val schema: Schema[Foo] = recursive {
        val foos = schema.optional[Foo]("foo", _.foo)
        struct(foos)(Foo.apply)
      }
    }
    checkSchema(Foo.schema)
  }

  test(header + "bijection") {
    case class Foo(x: Int)
    val schema: Schema[Foo] = bijection(int, Foo(_), _.x)
    checkSchema(schema)
  }

  test(header + "surjection") {
    val schema: Schema[Int] =
      int.refined(smithy.api.Range(None, Option(BigDecimal(1))))
    checkSchema(schema)
  }
}
