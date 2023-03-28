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
package schema

import cats.syntax.all._
import Schema._
import munit._

final class PartialSchemaSpec extends FunSuite {

  test("Structure schemas can be divided into partial components") {
    case class Foo(x: Int, y: Option[Int])
    val schema = struct(
      int.required[Foo]("x", _.x),
      int.optional[Foo]("y", _.y)
    )(Foo.apply)

    // We're splitting the schema into various components,
    // which we can use to decode bits of data from various locations
    // and reconcile later on.
    val xPartialSchema = schema.partial(_.label == "x")
    val yPartialSchema = schema.partial(_.label != "x")

    // These are two separate documents that the whole data
    // can't possibly be decoded from.
    val documentX = Document.obj("x" -> Document.fromInt(1))
    val documentY = Document.obj("y" -> Document.fromInt(1))

    import PartialSchema._
    (xPartialSchema, yPartialSchema) match {
      case (PartialMatch(xSchema), PartialMatch(ySchema)) =>
        val decoderX = Document.Decoder.fromSchema(xSchema)
        val decoderY = Document.Decoder.fromSchema(ySchema)

        val result =
          (decoderX.decode(documentX), decoderY.decode(documentY)).mapN {
            PartialData.unsafeReconcile(_, _)
          }

        assertEquals(result, Right(Foo(1, Some(1))))

      case (_, _) => fail("Expected partial matches")
    }
  }

  test("Partial schemas can be used to encode subsets of data") {
    case class Foo(x: Int, y: Option[Int])
    val schema = struct(
      int.required[Foo]("x", _.x),
      int.optional[Foo]("y", _.y)
    )(Foo.apply)

    // We're splitting the schema into various components,
    // which we can use to decode bits of data from various locations
    // and reconcile later on.
    val xPartialSchema = schema.partial(_.label == "x")
    val yPartialSchema = schema.partial(_.label != "x")

    // These are two separate documents that the whole data
    // can't possibly be decoded from.
    val documentX = Document.obj("x" -> Document.fromInt(1))
    val documentY = Document.obj("y" -> Document.fromInt(1))

    import PartialSchema._
    (xPartialSchema, yPartialSchema) match {
      case (PartialMatch(xSchema), PartialMatch(ySchema)) =>
        val encoderX = Document.Encoder.fromSchema(xSchema)
        val encoderY = Document.Encoder.fromSchema(ySchema)

        val input = PartialData.Total(Foo(1, Some(1)))
        assertEquals(encoderX.encode(input), documentX)
        assertEquals(encoderY.encode(input), documentY)

      case (_, _) => fail("Expected partial matches")
    }
  }

  test("Structure schemas can be divided into partial components") {
    case class Foo(x: Int, y: Option[Int])
    val schema = struct(
      int.required[Foo]("x", _.x),
      int.optional[Foo]("y", _.y)
    )(Foo.apply)

    // We're splitting the schema into various components,
    // which we can use to decode bits of data from various locations
    // and reconcile later on.
    val xPartialSchema = schema.partial(_.label == "x")
    val yPartialSchema = schema.partial(_.label != "x")

    // These are two separate documents that the whole data
    // can't possibly be decoded from.
    val documentX = Document.obj("x" -> Document.fromInt(1))
    val documentY = Document.obj("y" -> Document.fromInt(1))

    import PartialSchema._
    (xPartialSchema, yPartialSchema) match {
      case (PartialMatch(xSchema), PartialMatch(ySchema)) =>
        val decoderX = Document.Decoder.fromSchema(xSchema)
        val decoderY = Document.Decoder.fromSchema(ySchema)

        val result =
          (decoderX.decode(documentX), decoderY.decode(documentY)).mapN {
            PartialData.unsafeReconcile(_, _)
          }

        assertEquals(result, Right(Foo(1, Some(1))))

      case (_, _) => fail("Expected partial matches")
    }
  }

  test(
    "Payload-specialised partial schemas can be extracted from structure schemas"
  ) {
    case class Foo(x: List[Int], y: Int)
    val schema = struct(
      list(int).required[Foo]("x", _.x),
      int.required[Foo]("y", _.y)
    )(Foo.apply)

    // We're splitting the schema into various components. The first one is extracted
    // as a "payload" partial, which means that schema held by the first field
    // matching the predicate will be used as if it was a top level schema
    val xPartialSchema = schema.payloadPartial(_.label == "x")
    val yPartialSchema = schema.partial(_.label != "x")

    val documentX = Document.array(Document.fromInt(1), Document.fromInt(2))
    val documentY = Document.obj("y" -> Document.fromInt(1))

    import PartialSchema._
    (xPartialSchema, yPartialSchema) match {
      case (PartialMatch(xSchema), PartialMatch(ySchema)) =>
        val decoderX = Document.Decoder.fromSchema(xSchema)
        val decoderY = Document.Decoder.fromSchema(ySchema)

        val result =
          (decoderX.decode(documentX), decoderY.decode(documentY)).mapN {
            PartialData.unsafeReconcile(_, _)
          }

        assertEquals(result, Right(Foo(List(1, 2), 1)))

      case (_, _) => fail("Expected partial matches")
    }
  }

  test(
    "Computing a partial schema using a predicate that matches all fields returns the original schema"
  ) {
    case class Foo(x: Int, y: Option[Int])
    val schema = struct(
      int.required[Foo]("x", _.x),
      int.optional[Foo]("y", _.y)
    )(Foo.apply)

    val partialSchema = schema.partial(_ => true)
    assert(partialSchema == PartialSchema.TotalMatch(schema))
  }

  test(
    "Computing a partial schema using a predicate that matches no field returns no schema"
  ) {
    case class Foo(x: Int, y: Option[Int])
    val schema = struct(
      int.required[Foo]("x", _.x),
      int.optional[Foo]("y", _.y)
    )(Foo.apply)

    val partialSchema = schema.partial(_.label.startsWith("z"))
    assertEquals(partialSchema, PartialSchema.NoMatch[Foo]())
  }

  test(
    "Computing a partial schema from a non-struct schema returns no-schema"
  ) {
    val schema = list(int)

    val partialSchema = schema.partial(_.label.startsWith("z"))
    assertEquals(partialSchema, PartialSchema.NoMatch[List[Int]]())
  }

  test(
    "Payload-specialised partial schemas are considered total when the original schema had a single field"
  ) {
    case class Foo(x: List[Int])
    val schema = struct(
      list(int).required[Foo]("x", _.x)
    )(Foo.apply)

    // We're splitting the schema into various components. The first one is extracted
    // as a "payload" partial, which means that schema held by the first field
    // matching the predicate will be used as if it was a top level schema
    val xPartialSchema = schema.payloadPartial(_.label == "x")

    val documentX = Document.array(Document.fromInt(1), Document.fromInt(2))

    import PartialSchema._
    xPartialSchema match {
      case (TotalMatch(xSchema)) =>
        val decoderX = Document.Decoder.fromSchema(xSchema)

        val result = decoderX.decode(documentX)

        assert(result == Right(Foo(List(1, 2))))

      case _ => fail("Expected a total match")
    }
  }

}
