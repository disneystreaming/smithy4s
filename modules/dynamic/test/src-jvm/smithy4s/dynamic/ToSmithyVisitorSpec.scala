/*
 *  Copyright 2021-2025 Disney Streaming
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

import weaver._
import smithy4s.schema._
import smithy4s.schema.Schema._
import software.amazon.smithy.model.Model
import cats.kernel.Eq
import smithy4s.ShapeId
import smithy.api.JsonName
import java.util.UUID
import smithy4s.Timestamp
import smithy4s.Blob
import smithy.api.Length
import smithy4s.Newtype
import smithy.api.Pattern
import smithy4s.Service
import scala.io.Source

object ToSmithyVisitorSpec extends FunSuite {

  test("structure") {
    val smithy = """|namespace foo
                    |
                    |structure Test {
                    |  @required
                    |  a: String
                    |  @jsonName("bb")
                    |  b: Boolean
                    |  @required
                    |  c: Integer
                    |  @required
                    |  d: Short
                    |  @required
                    |  e: Long
                    |  @required
                    |  f: Float
                    |  @required
                    |  g: Double
                    |  @required
                    |  h: BigInteger
                    |  @required
                    |  i: BigDecimal
                    |  @required
                    |  j: Byte
                    |  @required
                    |  k: alloy#UUID
                    |  @required
                    |  l: Blob
                    |  @required
                    |  m: Timestamp
                    |}
                    |""".stripMargin

    case class Foo(
        a: String,
        b: Option[Boolean],
        c: Int,
        d: Short,
        e: Long,
        f: Float,
        g: Double,
        h: BigInt,
        i: BigDecimal,
        j: Byte,
        k: UUID,
        l: Blob,
        m: Timestamp
    )
    object Foo {
      implicit val schema: Schema[Foo] = {
        val a = string.required[Foo]("a", _.a)
        val b = boolean.optional[Foo]("b", _.b).addHints(JsonName("bb"))
        val c = int.required[Foo]("c", _.c)
        val d = short.required[Foo]("d", _.d)
        val e = long.required[Foo]("e", _.e)
        val f = float.required[Foo]("f", _.f)
        val g = double.required[Foo]("g", _.g)
        val h = bigint.required[Foo]("h", _.h)
        val i = bigdecimal.required[Foo]("i", _.i)
        val j = byte.required[Foo]("j", _.j)
        val k = uuid.required[Foo]("k", _.k)
        val l = blob.required[Foo]("l", _.l)
        val m = timestamp.required[Foo]("m", _.m)
        struct(a, b, c, d, e, f, g, h, i, j, k, l, m)(Foo.apply)
      }.withId(ShapeId("foo", "Test"))
    }
    runTest(smithy, Foo.schema)
  }

  test("union") {
    val smithy = """|namespace foo
                    |
                    |union Test {
                    |  a: String
                    |  b: Bee
                    |}
                    |
                    |structure Bee {
                    |  @required
                    |  name: String
                    |}
                    |""".stripMargin

    case class Bee(name: String)
    object Bee {
      implicit val schema: Schema[Bee] =
        struct(string.required[Bee]("name", _.name))(Bee.apply)
          .withId(ShapeId("foo", "Bee"))
    }
    sealed trait Test
    object Test {
      case class A(s: String) extends Test
      case class B(bee: Bee) extends Test
      implicit val schema: Schema[Test] = union(
        Alt[Test, String]("a", string, A(_), { case a: A => a.s }),
        Alt[Test, Bee]("b", Bee.schema, B(_), { case b: B => b.bee })
      )({
        case _: A => 0
        case _: B => 1
      }).withId(ShapeId("foo", "Test"))
    }

    runTest(smithy, Test.schema)
  }

  test("primitive and member hints") {
    val smithy = """|namespace foo
                    |
                    |@length(min: 1)
                    |string MyString
                    |
                    |structure Test {
                    |  @required
                    |  name: String
                    |  @pattern(".")
                    |  other: MyString
                    |}
                    |""".stripMargin

    case class MyString(value: String)
    object MyString {
      val underlyingSchema: Schema[String] = string
        .withId(ShapeId("foo", "MyString"))
        .addHints(Length(min = Some(1)))
      implicit val schema: Schema[MyString] =
        bijection(
          underlyingSchema,
          new Newtype.Make[String, MyString] {
            def to(a: String): MyString = MyString(a)
            def from(t: MyString): String = t.value
          }
        )
    }
    case class Test(name: String, other: Option[MyString])
    object Test {
      implicit val schema: Schema[Test] =
        struct(
          string.required[Test]("name", _.name),
          MyString.schema
            .optional[Test]("other", _.other)
            .addHints(Pattern("."))
        )(
          Test.apply(_, _)
        )
          .withId(ShapeId("foo", "Test"))
    }

    runTest(smithy, Test.schema)
  }

  test("full service test - one") {
    val in = Source.fromResource("kvstore.smithy").mkString
    runTest(in, smithy4s.example.KVStore)
  }

  private implicit val modelWrapperEq: Eq[ModelWrapper] = Eq.fromUniversalEquals

  private def runTest[A](in: String, schema: Schema[A])(implicit
      loc: SourceLocation
  ): Expectations = {
    val inModel =
      Model
        .assembler()
        .discoverModels()
        .addUnparsedModel("foo.smithy", in)
        .assemble()
        .unwrap()
    val out = Model
      .assembler()
      .discoverModels()
      .addModel(
        DynamicSchemaIndex.builder.addSchema(schema).build().toSmithyModel
      )
      .assemble()
      .unwrap()
    expect.eql(ModelWrapper(inModel), ModelWrapper(out))
  }

  private def runTest[Alg[_[_, _, _, _, _]]](
      in: String,
      service: Service[Alg]
  )(implicit
      loc: SourceLocation
  ): Expectations = {
    val inModel =
      Model
        .assembler()
        .discoverModels()
        .addUnparsedModel("foo.smithy", in)
        .assemble()
        .unwrap()
    val out = Model
      .assembler()
      .discoverModels()
      .addModel(
        DynamicSchemaIndex.builder.addService(service).build().toSmithyModel
      )
      .assemble()
      .unwrap()
    expect.eql(ModelWrapper(inModel), ModelWrapper(out))
  }

}
