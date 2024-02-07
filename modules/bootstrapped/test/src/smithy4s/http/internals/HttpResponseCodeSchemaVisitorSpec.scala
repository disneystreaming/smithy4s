/*
 *  Copyright 2021-2023 Disney Streaming
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
import smithy4s.schema.Schema

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

    val hints: Hints = Hints.empty

    case object OK extends StatusCode("TWOHUNDRED", "TWOHUNDRED", 200, Hints())

    case object REDIRECT
        extends StatusCode("THREEHUNDRED", "THREEHUNDRED", 300, Hints())

    val values: List[StatusCode] = List(
      OK,
      REDIRECT
    )
    implicit val schema: Schema[StatusCode] =
      Schema.intEnumeration(values).withId(id).addHints(hints)
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
    "applying HttpResponseCode works on an int enumeration"
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

  type StatusCodeNewType = StatusCodeNewType.Type

  object StatusCodeNewType extends Newtype[Int] {
    val id: ShapeId = ShapeId("smithy4s.example", "StatusCodeNewType")
    val hints: Hints = Hints(
      smithy.api.Box()
    )
    val underlyingSchema: Schema[Int] = int.withId(id).addHints(hints)
    implicit val schema: Schema[StatusCodeNewType] =
      bijection(underlyingSchema, asBijection)
  }

  case class SampleResponse2(code: StatusCodeNewType)

  object SampleResponse2 extends ShapeTag.Companion[SampleResponse2] {
    val id: ShapeId = ShapeId("smithy4s.example", "SampleResponse2")

    implicit val schema: Schema[SampleResponse2] =
      Schema
        .struct[SampleResponse2](
          StatusCodeNewType.schema
            .required[SampleResponse2]("code", _.code)
            .addHints(smithy.api.HttpResponseCode())
        )(SampleResponse2.apply)
        .withId(id)
  }

  test(
    "applying HttpResponseCode works on an int newtype"
  ) {
    val res: ResponseCodeExtractor[SampleResponse2] =
      SampleResponse2.schema.compile(visitor)
    val test = SampleResponse2(StatusCodeNewType(234))
    res match {
      case HttpResponseCodeSchemaVisitor.RequiredResponseCode(f) =>
        assert(f(test) == 234)
      case _ => fail("Expected RequiredResponseCode")
    }
  }

  case class RefinedStatusCode(value: Int)

  class RefinedStatusCodeFormat()

  object RefinedStatusCodeFormat
      extends ShapeTag.Companion[RefinedStatusCodeFormat] {
    val id: ShapeId = ShapeId("smithy4s.example", "RefinedStatusCodeFormat")
    val hints: Hints = Hints(
      smithy.api.Trait()
    )
    implicit val schema: Schema[RefinedStatusCodeFormat] =
      Schema.StructSchema(
        id,
        Hints.empty,
        Vector.empty,
        _ => new RefinedStatusCodeFormat()
      )
  }

  object RefinedStatusCode {

    private def isValidRefinedStatusCode(value: Int): Boolean =
      value > 100 && value < 600

    def apply(value: Int): Either[String, RefinedStatusCode] =
      if (isValidRefinedStatusCode(value)) Right(new RefinedStatusCode(value))
      else Left("RefinedStatusCode is not valid")

    implicit val provider
        : RefinementProvider[RefinedStatusCodeFormat, Int, RefinedStatusCode] =
      Refinement.drivenBy[RefinedStatusCodeFormat](
        RefinedStatusCode.apply,
        (e: RefinedStatusCode) => e.value
      )

    val schema = Schema.int
      .refined[RefinedStatusCode](new RefinedStatusCodeFormat())
      .withId(ShapeId("smithy4s.example", "RefinedStatusCode"))
  }

  case class SampleResponse3(code: RefinedStatusCode)

  object SampleResponse3 extends ShapeTag.Companion[SampleResponse3] {
    val id: ShapeId = ShapeId("smithy4s.example", "SampleResponse3")

    implicit val schema: Schema[SampleResponse3] =
      Schema
        .struct[SampleResponse3](
          RefinedStatusCode.schema
            .required[SampleResponse3]("code", _.code)
            .addHints(smithy.api.HttpResponseCode())
        )(SampleResponse3.apply)
        .withId(id)
  }

  test(
    "applying HttpResponseCode works on a refined int"
  ) {
    val res: ResponseCodeExtractor[SampleResponse3] =
      SampleResponse3.schema.compile(visitor)
    val test = SampleResponse3(RefinedStatusCode(234).toOption.get)
    res match {
      case HttpResponseCodeSchemaVisitor.RequiredResponseCode(f) =>
        assert(f(test) == 234)
      case _ => fail("Expected RequiredResponseCode")
    }
  }

  sealed trait Union1
  object Union1 extends ShapeTag.Companion[Union1] {
    case class Variant1(value: Int) extends Union1
    case class Variant2(value: String) extends Union1
    case class Variant3(value: SampleResponse1) extends Union1

    val id: ShapeId = ShapeId("smithy4s.example", "Union1")
    val hints: Hints = Hints(
    )
    implicit val schema: Schema[Union1] = Schema.UnionSchema(
      id,
      hints,
      Vector(
        Schema.int
          .biject(Variant1.apply(_))(_.value)
          .oneOf[Union1]("Variant1")
          .addHints(smithy.api.HttpResponseCode()),
        Schema.string
          .biject(Variant2.apply(_))(_.value)
          .oneOf[Union1]("Variant2"),
        SampleResponse1.schema
          .biject(Variant3.apply(_))(_.value)
          .oneOf[Union1]("Variant3")
      ),
      {
        case _: Variant1 => 1
        case _: Variant2 => 2
        case _: Variant3 => 3
      }
    )
  }

  test("applying HttpResponseCode works on a union with status codes") {
    val res: ResponseCodeExtractor[Union1] =
      Union1.schema.compile(visitor)
    res match {
      case HttpResponseCodeSchemaVisitor.OptionalResponseCode(f) =>
        assert(f(Union1.Variant1(234)) == Some(234))
        assert(f(Union1.Variant2("abc")) == None)
        assert(f(Union1.Variant3(SampleResponse1(StatusCode.OK))) == Some(200))
      case _ => fail("Expected RequiredResponseCode")
    }
  }

  sealed trait Union2
  object Union2 extends ShapeTag.Companion[Union2] {
    case class Variant1(value: Int) extends Union2
    case class Variant2(value: String) extends Union2

    val id: ShapeId = ShapeId("smithy4s.example", "Union1")
    val hints: Hints = Hints(
    )
    implicit val schema: Schema[Union2] = Schema.UnionSchema(
      id,
      hints,
      Vector(
        Schema.int.biject(Variant1.apply(_))(_.value).oneOf[Union2]("Variant1"),
        Schema.string
          .biject(Variant2.apply(_))(_.value)
          .oneOf[Union2]("Variant2")
      ),
      {
        case _: Variant1 => 1
        case _: Variant2 => 2
      }
    )
  }

  test("applying HttpResponseCode works on a union without status codes") {
    val res: ResponseCodeExtractor[Union2] =
      Union2.schema.compile(visitor)
    assert(res == HttpResponseCodeSchemaVisitor.NoResponseCode)
  }

}
