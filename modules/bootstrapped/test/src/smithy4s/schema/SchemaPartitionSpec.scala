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

final class SchemaPartitionSpec extends FunSuite {

  test(
    "Computing a partial schema using a predicate that matches all fields returns the original schema"
  ) {
    case class Foo(x: Int, y: Int)
    val schema = struct(
      int.required[Foo]("x", _.x),
      int.required[Foo]("y", _.y)
    )(Foo.apply)

    val partialSchema = schema.partition(_ => true)
    assert(partialSchema == SchemaPartition.TotalMatch(schema))
  }

  test(
    "Computing a partial schema using a predicate that matches no field returns no schema"
  ) {
    case class Foo(x: Int, y: Int)
    val schema = struct(
      int.required[Foo]("x", _.x),
      int.required[Foo]("y", _.y)
    )(Foo.apply)

    val partialSchema = schema.partition(_.label.startsWith("z"))
    assertEquals(partialSchema, SchemaPartition.NoMatch[Foo]())
  }

  test(
    "Computing a partial schema from a non-struct schema returns no-schema"
  ) {
    val schema = list(int)

    val partialSchema = schema.partition(_.label.startsWith("z"))
    assertEquals(partialSchema, SchemaPartition.NoMatch[List[Int]]())
  }

  test("Structure schemas can be divided into partial components") {
    case class Foo(x: Int, y: Int)
    val schema = struct(
      int.required[Foo]("x", _.x),
      int.required[Foo]("y", _.y)
    )(Foo.apply)

    // We're splitting the schema into various components,
    // which we can use to decode bits of data from various locations
    // and reconcile later on.
    val xPartialSchema = schema.partition(_.label == "x")

    // These are two separate documents that the whole data
    // can't possibly be decoded from.
    val documentX = Document.obj("x" -> Document.fromInt(1))
    val documentY = Document.obj("y" -> Document.fromInt(2))

    import SchemaPartition._
    xPartialSchema match {
      case SplittingMatch(xSchema, ySchema) =>
        val decoderX = Document.Decoder.fromSchema(xSchema)
        val decoderY = Document.Decoder.fromSchema(ySchema)

        val result =
          (decoderX.decode(documentX), decoderY.decode(documentY)).mapN {
            PartialData.unsafeReconcile(_, _)
          }

        assertEquals(result, Right(Foo(1, 2)))

      case _ => fail("Expected partial matches")
    }
  }

  test("Partial schemas can be used to encode subsets of data") {
    case class Foo(x: Int, y: Int)
    val schema = struct(
      int.required[Foo]("x", _.x),
      int.required[Foo]("y", _.y)
    )(Foo.apply)

    // We're splitting the schema into various components,
    // which we can use to decode bits of data from various locations
    // and reconcile later on.
    val xPartialSchema = schema.partition(_.label == "x")

    // These are two separate documents that the whole data
    // can't possibly be decoded from.
    val documentX = Document.obj("x" -> Document.fromInt(1))
    val documentY = Document.obj("y" -> Document.fromInt(2))

    import SchemaPartition._
    xPartialSchema match {
      case (SplittingMatch(xSchema, ySchema)) =>
        val encoderX = Document.Encoder.fromSchema(xSchema)
        val encoderY = Document.Encoder.fromSchema(ySchema)

        val input = PartialData.Total(Foo(1, 2))
        assertEquals(encoderX.encode(input), documentX)
        assertEquals(encoderY.encode(input), documentY)

      case _ => fail("Expected partial matches")
    }
  }

  test("PartialData.unsafeReconcile is order independent") {
    case class Foo(x: Int, y: Int)
    val schema = struct(
      int.required[Foo]("x", _.x),
      int.required[Foo]("y", _.y)
    )(Foo.apply)

    val xPartialSchema = schema.partition(_.label == "x")
    val documentX = Document.obj("x" -> Document.fromInt(1))
    val documentY = Document.obj("y" -> Document.fromInt(2))

    import SchemaPartition._
    (xPartialSchema) match {
      case (SplittingMatch(xSchema, ySchema)) =>
        val decoderX = Document.Decoder.fromSchema(xSchema)
        val decoderY = Document.Decoder.fromSchema(ySchema)

        val result =
          (decoderX.decode(documentX), decoderY.decode(documentY)).mapN {
            (px, py) =>
              // Swapping x and y
              PartialData.unsafeReconcile(py, px)
          }

        assertEquals(result, Right(Foo(1, 2)))

      case _ => fail("Expected partial matches")
    }
  }

  test(
    "Payload-specialised partial schemas help decode fields as if they were top-level data"
  ) {
    case class Foo(x: List[Int], y: Int)
    val schema = struct(
      list(int).required[Foo]("x", _.x),
      int.required[Foo]("y", _.y)
    )(Foo.apply)

    // We're splitting the schema into various components. The first one is extracted
    // as a "payload" partial, which means that schema held by the first field
    // matching the predicate will be used as if it was a top level schema
    val xPartialSchema = schema.findPayload(_.label == "x")

    // Note the absence of `x` document field below.
    val documentX = Document.array(Document.fromInt(1), Document.fromInt(2))
    val documentY = Document.obj("y" -> Document.fromInt(1))

    import SchemaPartition._
    xPartialSchema match {
      case SplittingMatch(xSchema, ySchema) =>
        val decoderX = Document.Decoder.fromSchema(xSchema)
        val decoderY = Document.Decoder.fromSchema(ySchema)

        val result =
          (decoderX.decode(documentX), decoderY.decode(documentY)).mapN {
            PartialData.unsafeReconcile(_, _)
          }

        assertEquals(result, Right(Foo(List(1, 2), 1)))

      case _ => fail("Expected partial matches")
    }
  }

  test(
    "Payload-specialised partial schemas are considered total when the original schema had a single field"
  ) {
    case class Foo(x: List[Int])
    val schema = struct(
      list(int).required[Foo]("x", _.x)
    )(Foo.apply)

    val xPartialSchema = schema.findPayload(_.label == "x")

    val documentX = Document.array(Document.fromInt(1), Document.fromInt(2))

    import SchemaPartition._
    xPartialSchema match {
      case (TotalMatch(xSchema)) =>
        val originalDecoder = Document.Decoder.fromSchema(schema)
        val payloadDecoder = Document.Decoder.fromSchema(xSchema)

        val orignalResult =
          originalDecoder.decode(Document.obj("x" -> documentX))
        val payloadResult = payloadDecoder.decode(documentX)

        assertEquals(payloadResult, Right(Foo(List(1, 2))))
        assertEquals(payloadResult, orignalResult)

      case _ => fail("Expected a total match")
    }
  }

  test(
    "An arbitrary number of PartialData instances can be reconciled together"
  ) {
    case class Foo(a: Int, b: Int, c: Int, d: Int)
    val schema = struct(
      int.required[Foo]("a", _.a),
      int.required[Foo]("b", _.b),
      int.required[Foo]("c", _.c),
      int.required[Foo]("d", _.d)
    )(Foo.apply)

    def multiPartitionedDecoder[A](schema: Schema[A])(
        predicates: Field[_, _] => Boolean*
    ): List[Document] => Either[codecs.PayloadError, A] = {
      val allDecoders: List[Document.Decoder[PartialData[A]]] =
        predicates.toList.map(schema.partition(_)).collect {
          case SchemaPartition.SplittingMatch(matching, _) =>
            Document.Decoder.fromSchema(matching)
        }
      (documents: List[Document]) =>
        allDecoders
          .zip(documents)
          .traverse { case (decoder, partialDocument) =>
            decoder.decode(partialDocument)
          }
          .map(partialResults =>
            PartialData.unsafeReconcile(partialResults: _*)
          )

    }

    val multiDecoder = multiPartitionedDecoder(schema)(
      _.label == "a",
      _.label == "b",
      _.label == "c",
      _.label == "d"
    )

    val result = multiDecoder(
      List(
        Document.obj("a" -> Document.fromInt(1)),
        Document.obj("b" -> Document.fromInt(2)),
        Document.obj("c" -> Document.fromInt(3)),
        Document.obj("d" -> Document.fromInt(4))
      )
    )

    assertEquals(result, Right(Foo(1, 2, 3, 4)))
  }

}
