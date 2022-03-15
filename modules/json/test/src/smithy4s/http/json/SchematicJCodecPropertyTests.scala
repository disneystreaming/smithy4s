/*
 *  Copyright 2021 Disney Streaming
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

package smithy4s.http.json

import cats.Show
import cats.effect.IO
import com.github.plokhotnyuk.jsoniter_scala.core._
import org.scalacheck.Gen
import smithy4s.ByteArray
import smithy4s.scalacheck.DynData
import smithy.api.Length
import smithy.api.Range
import smithy4s.Hints
import smithy4s.schema._
import smithy4s.syntax._
import smithy4s.http.PayloadError
import smithy4s.scalacheck._
import weaver._
import weaver.scalacheck._
import codecs.schematicJCodec

object SchematicJCodecPropertyTests extends SimpleIOSuite with Checkers {

  override val maxParallelism = 1

  override val checkConfig: CheckConfig =
    super.checkConfig.copy(perPropertyParallelism = 1)

  val genSchemaData: Gen[(Schema[DynData], Any)] = for {
    schema <- SchemaGenerator.genSchema(2, 2)
    data <- schema.compile(smithy4s.scalacheck.SchematicGen)
  } yield (schema -> data)

  implicit val schemaAndDataShow: Show[(Schema[DynData], Any)] =
    Show.fromToString

  implicit val schemaDataAndHintsShow: Show[(Hints, Schema[DynData], Any)] =
    Show.fromToString

  loggedTest("Json roundtrip works for the whole metamodel") { log =>
    // We're randomly generating a schema, and using it
    // to randomly generate compliant data, and
    // asserting roundtrip there.
    forall(genSchemaData) { schemaAndData =>
      val (schema, data) = schemaAndData
      implicit val codec: JCodec[Any] =
        schema.compile(schematicJCodec).get
      val schemaStr = schema.compile(smithy4s.schema.SchematicRepr)
      val json = writeToString(data)
      val config = ReaderConfig.withThrowReaderExceptionWithStackTrace(true)
      val result = scala.util.Try(readFromString[Any](json, config)).toEither
      result.left.foreach(_.printStackTrace())
      val e = exists(result)(d => expect(d == data))
      if (e.run.isInvalid) {
        for {
          _ <- log.debug("data: " + data)
          _ <- log.debug("schema: " + schemaStr)
          _ <- log.debug("json: " + json)
          _ <- log.debug("roundTrip: " + result.toString())
        } yield e
      } else IO.pure(e)
    }

  }

  test("constraint validation") {
    val gen = for {
      (hint, sch) <- genSchemaWithHints
      data <- sch.compile(smithy4s.scalacheck.SchematicGen)
    } yield (Hints(hint), sch, data)
    forall(gen) { case (hints, schema, data) =>
      val hint = hints.all.head
      implicit val codec: JCodec[Any] =
        schema.compile(schematicJCodec).get
      val json = writeToString(data)
      val config = ReaderConfig.withThrowReaderExceptionWithStackTrace(true)
      val result = scala.util.Try(readFromString[Any](json, config)).toEither
      result match {
        case Right(_) =>
          hint.value match {
            case Length(min, max) =>
              val size: Int = data match {
                case m: Map[_, _] => m.size
                case l: List[_]   => l.size
                case s: String    => s.size
                case b: ByteArray => b.array.size
              }
              expect(min.forall(_ <= size)) && expect(max.forall(_ >= size))
            case Range(min, max) =>
              val value = BigDecimal(data.toString)
              expect(min.forall(_ <= value)) && expect(
                max.forall(_ >= value)
              )
          }
        case Left(PayloadError(_, _, message)) =>
          hint.value match {
            case Length(min, max) =>
              expect(min.isEmpty || message.contains(min.get.toString)) &&
                expect(max.isEmpty || message.contains(max.get.toString))
            case Range(min, max) =>
              expect(min.isEmpty || message.contains(min.get.toString)) &&
                expect(max.isEmpty || message.contains(max.get.toString))
          }
        case _ => failure("result should have matched one of the above cases")
      }
    }
  }

  val genSchemaWithHints: Gen[(smithy4s.Hint, Schema[DynData])] = {
    for {
      min <- Gen.option(Gen.long)
      max <- Gen.option(
        Gen.chooseNum(min.getOrElse(Long.MinValue), Long.MaxValue)
      )
      hint <- Gen.oneOf[smithy4s.Hint](
        Length(min, max),
        Range(min.map(BigDecimal(_)), max.map(BigDecimal(_)))
      )
      schema <- genSchemaWithConstraints(hint)
    } yield (hint, schema)
  }

  def genSchemaWithConstraints(
      hint: smithy4s.Hint
  ): Gen[Schema[DynData]] = {
    def lengthGen(l: Length) = Gen.oneOf(
      Vector(
        (string),
        map(string, string),
        string,
        bytes
      ).map(_.addHints(l)).asInstanceOf[Vector[Schema[DynData]]]
    )

    def rangeGen(r: Range) = Gen.oneOf(
      Vector(
        byte,
        short,
        int,
        long,
        float,
        double,
        bigdecimal,
        bigint
      ).map(_.addHints(r)).asInstanceOf[Vector[Schema[DynData]]]
    )
    hint.value match {
      case l: Length => lengthGen(l)
      case r: Range  => rangeGen(r)
    }
  }

}
