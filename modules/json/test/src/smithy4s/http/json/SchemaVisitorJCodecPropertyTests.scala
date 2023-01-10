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
package http.json

import cats.Show
import com.github.plokhotnyuk.jsoniter_scala.core._
import org.scalacheck.Gen
import smithy.api.Length
import smithy.api.Range
import smithy4s.ByteArray
import smithy4s.Hints
import smithy4s.http.PayloadError
import smithy4s.scalacheck.DynData
import smithy4s.scalacheck._
import smithy4s.schema._
import smithy4s.schema.Schema._
import munit._

import codecs.schemaVisitorJCodec
import org.scalacheck.Prop
import Prop._

class SchemaVisitorJCodecPropertyTests() extends FunSuite with ScalaCheckSuite {

  val genSchemaData: Gen[(Schema[DynData], Any)] = for {
    schema <- SchemaGenerator.genSchema(2, 2)
    data <- schema.compile(smithy4s.scalacheck.SchemaVisitorGen)
  } yield (schema -> data)

  implicit val schemaAndDataShow: Show[(Schema[DynData], Any)] =
    Show.fromToString

  implicit val schemaDataAndHintsShow: Show[(Hints, Schema[DynData], Any)] =
    Show.fromToString

  property("Json roundtrip works for the whole metamodel") {
    // We're randomly generating a schema, and using it
    // to randomly generate compliant data, and
    // asserting roundtrip there.
    forAll(genSchemaData) { schemaAndData =>
      val (schema, data) = schemaAndData
      implicit val codec: JCodec[Any] =
        schema.compile(schemaVisitorJCodec(CompilationCache.nop[JCodec]))
      val schemaStr =
        schema.compile(smithy4s.internals.SchemaDescriptionDetailed)
      val json = writeToString(data)
      val config = ReaderConfig.withThrowReaderExceptionWithStackTrace(true)
      val result = scala.util.Try(readFromString[Any](json, config)).toEither
      result.left.foreach(_.printStackTrace())
      val prop = Prop(result == Right(data))
      prop.label(s"""|data: $data
                     |schema: $schemaStr
                     |json: $json
                     |roundTip: $result
                     |""".stripMargin)
    }

  }

  property("constraint validation") {
    val gen = for {
      (hint, sch) <- genSchemaWithHints
      data <- sch.compile(smithy4s.scalacheck.SchemaVisitorGen)
    } yield (Hints(hint), sch, data)
    forAll(gen) { case (hints, schema, data) =>
      val hint = hints.all.collect { case b: Hints.Binding.StaticBinding[_] =>
        b
      }.head
      implicit val codec: JCodec[Any] =
        schema.compile(schemaVisitorJCodec(CompilationCache.nop[JCodec]))
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
              expect(min.forall(_ <= size))
              expect(max.forall(_ >= size))
            case Range(min, max) =>
              val value = BigDecimal(data.toString)
              expect(min.forall(_ <= value))
              expect(max.forall(_ >= value))
          }
        case Left(PayloadError(_, _, message)) =>
          hint.value match {
            case Length(min, max) =>
              expect(min.isEmpty || message.contains(min.get.toString))
              expect(max.isEmpty || message.contains(max.get.toString))
            case Range(min, max) =>
              expect(min.isEmpty || message.contains(min.get.toString))
              expect(max.isEmpty || message.contains(max.get.toString))
          }
        case _ => fail("result should have matched one of the above cases")
      }
    }
  }

  val genSchemaWithHints: Gen[(smithy4s.Hint, Schema[DynData])] = {
    for {
      min <- Gen.option(Gen.chooseNum(0, 10))
      max <- Gen.option(
        Gen.chooseNum(min.getOrElse(0) + 10, min.getOrElse(0) + 20)
      )
      hint <- Gen.oneOf[smithy4s.Hint](
        Length(min.map(_.toLong), max.map(_.toLong)),
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
        string.validated[Length](l),
        map(string, string).validated[Length](l),
        string.validated[Length](l),
        bytes.validated[Length](l)
      ).asInstanceOf[Vector[Schema[DynData]]]
    )

    def rangeGen(r: Range) = Gen.oneOf(
      Vector(
        short.validated[Range](r),
        int.validated[Range](r),
        long.validated[Range](r),
        float.validated[Range](r),
        double.validated[Range](r),
        bigdecimal.validated[Range](r),
        bigint.validated[Range](r)
      ).asInstanceOf[Vector[Schema[DynData]]]
    )
    hint match {
      case Hints.Binding.StaticBinding(_, l: Length) => lengthGen(l)
      case Hints.Binding.StaticBinding(_, r: Range)  => rangeGen(r)
      case _ => Gen.const(int.asInstanceOf[Schema[Any]])
    }
  }

}
