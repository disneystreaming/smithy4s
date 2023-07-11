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
import smithy4s.Enumeration
import smithy4s.ShapeId
import smithy4s.Hints
import smithy4s.Lazy
import smithy4s.Bijection
import smithy4s.Refinement
import smithy4s.ShapeTag
import cats.syntax.all._
import Schema._

class HintsTransformationSpec() extends FunSuite {

  def header(
      tpe: String
  ): String = "Transitive hint transformation is applied to all layers: " + tpe

  test(header("primitive")) {
    implicit val schema: Schema[Int] = Schema.int
    checkSchema(1, 1)
  }

  test(header("list")) {
    implicit val schema: Schema[List[Int]] = Schema.list(Schema.int)
    // 3 primitives, 1 list
    checkSchema(List(1, 2, 3), 4)
  }

  test(header("map")) {
    implicit val schema: Schema[Map[Int, Int]] = map(int, int)
    // 2 keys, 2 values, 1 map
    checkSchema(Map(1 -> 1, 2 -> 2), 5)
  }

  test(header("enum")) {
    sealed abstract class FooBar(val stringValue: String, val intValue: Int)
        extends smithy4s.Enumeration.Value {
      override type EnumType = FooBar
      val name = stringValue
      val value = stringValue
      val hints = Hints.empty
      def enumeration: Enumeration[EnumType] = FooBar

    }
    object FooBar extends Enumeration[FooBar] {
      def hints = Hints.empty
      def id = ShapeId("", "FooBar")
      def values: List[FooBar] = List(Foo)

      case object Foo extends FooBar("foo", 0)

      implicit val schema: Schema[FooBar] =
        stringEnumeration[FooBar](List(Foo))
    }
    // 1 for the enum, 1 for the enum value
    checkSchema(FooBar.Foo: FooBar, 2)
  }

  test(header("struct")) {
    case class Foo(x: Int, y: Option[Int])
    implicit val schema = struct(
      int.required[Foo]("x", _.x),
      int.optional[Foo]("y", _.y)
    )(Foo.apply)
    // One for the case class, one for the x field
    checkSchema(Foo(1, None), 2)
    // One for the case class, one for the x field, one for the y field
    checkSchema(Foo(1, Some(1)), 3)
  }

  test(header("struct")) {
    case class Foo(x: Int, y: Option[Int])
    implicit val schema: Schema[Foo] = struct(
      int.required[Foo]("x", _.x),
      int.optional[Foo]("y", _.y)
    )(Foo.apply)
    // One for the case class, one for the x field
    checkSchema(Foo(1, None), 2)
    // One for the case class, one for the x field, one for the y field
    checkSchema(Foo(1, Some(1)), 3)
  }

  test(header("union")) {
    type Foo = Either[Int, String]
    val left = int.oneOf[Foo]("left", Left(_))
    val right = string.oneOf[Foo]("right", Right(_))
    implicit val schema: Schema[Foo] = union(left, right) {
      case Left(int)     => left(int)
      case Right(string) => right(string)
    }

    // One for the union, one for the union member
    checkSchema(Left(1): Foo, 2)
  }

  test(header("bijection")) {
    case class Foo(x: Int)
    implicit val schema: Schema[Foo] = bijection(int, Foo(_), _.x)
    checkSchema(Foo(1), 1)
  }

  test(header("bijection")) {
    implicit val schema: Schema[Int] =
      int.refined(smithy.api.Range(None, Option(BigDecimal(1))))
    checkSchema(1, 1)
  }

  test(header("recursive")) {
    case class Foo(foo: Option[Foo])
    object Foo {
      implicit val schema: Schema[Foo] = recursive {
        val foos = schema.optional[Foo]("foo", _.foo)
        struct(foos)(Foo.apply)
      }
    }
    checkSchema(Foo(None), 1)
    checkSchema(Foo(Some(Foo(None))), 2)
    checkSchema(Foo(Some(Foo(Some(Foo(None))))), 3)
  }

  test(header("nullable")) {
    implicit val schema: Schema[Option[Int]] = int.nullable
    checkSchema(1.some, 1)
    checkSchema(none[Int], expectedTransitive = 0, expectedLocal = 0)
  }

  case class Mark()
  object Mark extends ShapeTag.Companion[Mark] {
    implicit val schema: Schema[Mark] =
      Schema.constant(Mark()).withId(ShapeId("test", "Mark"))
    def id: ShapeId = schema.shapeId
  }

  // We're testing that all layers have received a mark, by counting the marks
  // when a runtime instance traverses the corresponding layers.
  //
  // It is important that we test against runtime values, to ensure that
  // the transformation works correctly with unions (which is the trickiest)
  def checkSchema[A: Schema](
      value: A,
      expectedTransitive: Int,
      expectedLocal: Int = 1
  )(implicit
      loc: Location
  ): Unit = {
    val countLocal = implicitly[Schema[A]]
      .transformHintsLocally(_ ++ Hints(Mark()))
      .compile(CountVisitor)

    val countTransitive = implicitly[Schema[A]]
      .transformHintsTransitively(_ ++ Hints(Mark()))
      .compile(CountVisitor)

    val localMsg = "Unexpected count of marks after local transformation"
    assertEquals(countLocal(value), expectedLocal, localMsg)
    val transitiveMsg =
      "Unexpected count of marks after transitive transformation"
    assertEquals(countTransitive(value), expectedTransitive, transitiveMsg)
  }

  private def count(hints: Hints): Int = if (hints.has[Mark]) 1 else 0

  // Counts how much time an instance traverses "marked" datatypes
  type Count[A] = A => Int
  object CountVisitor extends SchemaVisitor[Count] { compile =>
    def primitive[P](
        shapeId: ShapeId,
        hints: Hints,
        tag: Primitive[P]
    ): Count[P] = _ => count(hints)

    def collection[C[_], A](
        shapeId: ShapeId,
        hints: Hints,
        tag: CollectionTag[C],
        member: Schema[A]
    ): Count[C[A]] = {
      val cMember = compile(member)
      ca => {
        count(hints) + tag.iterator(ca).toList.foldMap(cMember(_))
      }
    }

    def map[K, V](
        shapeId: ShapeId,
        hints: Hints,
        key: Schema[K],
        value: Schema[V]
    ): Count[Map[K, V]] = {
      val ck = compile(key)
      val cv = compile(value)
      mkv => {
        count(hints) + mkv.toList.foldMap { case (k, v) => ck(k) + cv(v) }
      }
    }

    def enumeration[E](
        shapeId: ShapeId,
        hints: Hints,
        tag: EnumTag,
        values: List[EnumValue[E]],
        total: E => EnumValue[E]
    ): Count[E] = { e =>
      count(hints) + count(total(e).hints)
    }

    def struct[S](
        shapeId: ShapeId,
        hints: Hints,
        fields: Vector[Field[S, _]],
        make: IndexedSeq[Any] => S
    ): Count[S] = {
      def countField[AA](field: smithy4s.schema.Field[S, AA]) =
        compile(field.schema).compose(field.get)
      s => count(hints) + fields.foldMap(countField(_)(s))
    }

    def union[U](
        shapeId: ShapeId,
        hints: Hints,
        alternatives: Vector[Alt[U, _]],
        dispatch: Alt.Dispatcher[U]
    ): Count[U] = {
      val countU = dispatch.compile(new Alt.Precompiler[Count] {
        def apply[A](label: String, instance: Schema[A]): Count[A] =
          compile(instance)
      })
      u => countU(u) + count(hints)
    }

    def biject[A, B](schema: Schema[A], bijection: Bijection[A, B]): Count[B] =
      compile(schema).compose(bijection.from)

    def refine[A, B](
        schema: Schema[A],
        refinement: Refinement[A, B]
    ): Count[B] = compile(schema).compose(refinement.from)

    def lazily[A](suspend: Lazy[Schema[A]]): Count[A] = {
      lazy val underlying = compile(suspend.value)
      a => underlying(a)
    }

    def nullable[A](schema: Schema[A]): Count[Option[A]] = {
      val count = compile(schema)
      locally {
        case Some(a) => count(a)
        case None    => 0
      }
    }

  }

}
