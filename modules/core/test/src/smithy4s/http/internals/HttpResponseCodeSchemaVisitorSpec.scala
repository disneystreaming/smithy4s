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
package http.internals

import smithy4s.Schema._
import smithy4s.http.internals.HttpResponseCodeSchemaVisitor.ResponseCodeExtractor
import munit._

class HttpResponseCodeSchemaVisitorSpec() extends FunSuite {

  val visitor = new HttpResponseCodeSchemaVisitor()
  case class SampleResponse(code: Int)
  object SampleResponse extends ShapeTag.Companion[SampleResponse] {
    implicit val schema: Schema[SampleResponse] =
      Schema
        .struct[SampleResponse](
          int
            .required[SampleResponse]("code", _.code)
            .addHints(smithy.api.HttpResponseCode())
        )(SampleResponse.apply)
        .withId("", "SampleResponse")

    val id: ShapeId = ShapeId("", "SampleResponse")
  }

  test("applying HttpResponseCode works on an int primitive") {
    val res: ResponseCodeExtractor[SampleResponse] =
      SampleResponse.schema.compile(visitor)
    val tes = SampleResponse(200)
    res match {
      case HttpResponseCodeSchemaVisitor.RequiredResponseCode(f) =>
        assert(f(tes) == 200)
      case _ => fail("Expected RequiredResponseCode")
    }

  }

  sealed abstract class StatusCode(
      _value: String,
      _name: String,
      _intValue: Int,
      _hints: Hints
  ) extends Enumeration.Value {
    override type EnumType = StatusCode
    override val value: String = _value
    override val name: String = _name
    override val intValue: Int = _intValue
    override val hints: Hints = _hints

    override def enumeration: Enumeration[EnumType] = StatusCode

    @inline final def widen: StatusCode = this
  }

  object StatusCode
      extends Enumeration[StatusCode]
      with ShapeTag.Companion[StatusCode] {
    val id: ShapeId = ShapeId("smithy4s.example", "StatusCode")

    val hints: Hints = Hints(
      IntEnum()
    )

    case object OK extends StatusCode("TWOHUNDRED", "TWOHUNDRED", 200, Hints())

    case object REDIRECT
        extends StatusCode("THREEHUNDRED", "THREEHUNDRED", 300, Hints())

    val values: List[StatusCode] = List(
      OK,
      REDIRECT
    )
    implicit val schema: Schema[StatusCode] =
      enumeration(values).withId(id).addHints(hints)
  }

  case class SampleResponse1(code: StatusCode)

  object SampleResponse1 extends ShapeTag.Companion[SampleResponse1] {
    implicit val schema: Schema[SampleResponse1] =
      Schema
        .struct[SampleResponse1](
          StatusCode.schema
            .required[SampleResponse1]("code", _.code)
            .addHints(smithy.api.HttpResponseCode())
        )(SampleResponse1.apply)
        .withId("", "SampleResponse")

    val id: ShapeId = ShapeId("", "SampleResponse")
  }

  test(
    "applying HttpResponseCode works on an enumeration with an intEnum hint"
  ) {

    val res: ResponseCodeExtractor[SampleResponse1] =
      SampleResponse1.schema.compile(visitor)
    val test = SampleResponse1(StatusCode.OK)
    res match {
      case HttpResponseCodeSchemaVisitor.RequiredResponseCode(f) =>
        assert(f(test) == 200)
      case _ => fail("Expected RequiredResponseCode")
    }

  }

}
