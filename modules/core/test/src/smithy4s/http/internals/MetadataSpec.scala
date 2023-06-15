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

import cats.syntax.all._
import smithy4s.Schema
import smithy4s.Timestamp
import smithy4s.example.Headers
import smithy4s.example.HeadersWithDefaults
import smithy4s.example.PathParams
import smithy4s.example.Queries
import smithy4s.example.QueriesWithDefaults
import smithy4s.example.ValidationChecks
import smithy4s.http.CaseInsensitive
import smithy4s.http.HttpBinding
import smithy4s.http.Metadata
import smithy4s.http.MetadataError
import smithy4s.internals.InputOutput
import munit._

class MetadataSpec() extends FunSuite {

  implicit val queriesSchema: Schema[Queries] =
    Queries.schema.addHints(InputOutput.Input.widen)
  implicit val headersSchema: Schema[Headers] =
    Headers.schema.addHints(InputOutput.Input.widen)
  implicit val queriesDefaultSchema: Schema[QueriesWithDefaults] =
    QueriesWithDefaults.schema.addHints(InputOutput.Input.widen)
  implicit val headersDefaultSchema: Schema[HeadersWithDefaults] =
    HeadersWithDefaults.schema.addHints(InputOutput.Input.widen)
  implicit val pathParamsSchema: Schema[PathParams] =
    PathParams.schema.addHints(InputOutput.Input.widen)
  implicit val validationChecksSchema: Schema[ValidationChecks] =
    ValidationChecks.schema.addHints(InputOutput.Input.widen)

  def checkRoundTrip[A](a: A, expectedEncoding: Metadata)(implicit
      s: Schema[A],
      loc: Location
  ): Unit = {
    val encoded = Metadata.encode(a)
    val result = Metadata
      .decode[A](encoded)
      .left
      .map(_.getMessage())
    expect.same(encoded, expectedEncoding)
    expect(result == Right(a))
  }

  def checkQueryRoundTrip[A](
      initial: A,
      expectedEncoding: Metadata,
      finished: A
  )(implicit
      s: Schema[A],
      loc: Location
  ): Unit = {
    val encoded = Metadata.encode(initial)
    val result = Metadata
      .decode[A](encoded)
      .left
      .map(_.getMessage())
    expect.same(encoded, expectedEncoding)
    expect(result == Right(finished))
  }

  def checkRoundTripDefault[A](expectedDecoded: A)(implicit
      s: Schema[A],
      loc: Location
  ): Unit = {
    val result = Metadata
      .decode[A](Metadata.empty)
      .left
      .map(_.getMessage())
    expect.same(result, Right(expectedDecoded))
  }

  def checkRoundTripError[A](a: A, expectedEncoding: Metadata, message: String)(
      implicit
      s: Schema[A],
      loc: Location
  ): Unit = {
    val encoded = Metadata.encode(a)
    val result = Metadata
      .decode[A](encoded)
      .left
      .map(_.getMessage())
    expect.same(encoded, expectedEncoding)
    expect.same(result, Left(message))
  }

  val epochString = "1970-01-01T00:00:00Z"

  val constraintMessage1 =
    "length required to be >= 1 and <= 10, but was 11"

  val constraintMessage2 =
    if (!Platform.isJS) "Input must be >= 1.0 and <= 10.0, but was 11.0"
    else "Input must be >= 1 and <= 10, but was 11"

  // ///////////////////////////////////////////////////////////
  // QUERY PARAMETERS
  // ///////////////////////////////////////////////////////////
  test("String query parameter") {
    val queries = Queries(str = Some("hello"))
    val finished =
      Queries(str = Some("hello"), slm = Some(Map("str" -> "hello")))
    val expected = Metadata(query = Map("str" -> List("hello")))
    checkQueryRoundTrip(queries, expected, finished)
  }

  test("String query parameter with default") {
    val expectedDecoded = QueriesWithDefaults(dflt = "test")
    checkRoundTripDefault(expectedDecoded)
  }

  test("String length constraint violation") {
    val string = "1" * 11
    val queries = ValidationChecks(str = Some(string))
    val expected = Metadata(query = Map("str" -> List(string)))
    checkRoundTripError(
      queries,
      expected,
      s"Field str, found in Query parameter str, failed constraint checks with message: $constraintMessage1"
    )
  }

  test("List length constraint violation") {
    val list = List.fill(11)("str")
    val queries = ValidationChecks(lst = Some(list))
    val expected = Metadata(query = Map("lst" -> list))
    checkRoundTripError(
      queries,
      expected,
      s"Field lst, found in Query parameter lst, failed constraint checks with message: $constraintMessage1"
    )
  }

  test("Integer range constraint violation") {
    val i = 11
    val queries = ValidationChecks(int = Some(i))
    val expected = Metadata(query = Map("int" -> List(i.toString)))
    checkRoundTripError(
      queries,
      expected,
      s"Field int, found in Query parameter int, failed constraint checks with message: $constraintMessage2"
    )
  }

  test("Integer query parameter") {
    val queries = Queries(int = Some(123))
    val finished = Queries(int = Some(123), slm = Some(Map("int" -> "123")))
    val expected = Metadata(query = Map("int" -> List("123")))
    checkQueryRoundTrip(queries, expected, finished)
  }

  test("Boolean query parameter") {
    val queries = Queries(b = Some(true))
    val finished = Queries(b = Some(true), slm = Some(Map("b" -> "true")))
    val expected = Metadata(query = Map("b" -> List("true")))
    checkQueryRoundTrip(queries, expected, finished)
  }

  test("timestamp query parameters (no format)") {
    val ts = Timestamp(1970, 1, 1, 0, 0, 0)
    val queries = Queries(ts1 = Some(ts))
    val finished =
      Queries(ts1 = Some(ts), slm = Some(Map("ts1" -> epochString)))
    val expected = Metadata(query = Map("ts1" -> List(epochString)))
    checkQueryRoundTrip(queries, expected, finished)
  }

  test("timestamp query parameters (date-time format)") {
    val ts = Timestamp(1970, 1, 1, 0, 0, 0)
    val queries = Queries(ts2 = Some(ts))
    val finished =
      Queries(ts2 = Some(ts), slm = Some(Map("ts2" -> epochString)))
    val expected = Metadata(query = Map("ts2" -> List(epochString)))
    checkQueryRoundTrip(queries, expected, finished)
  }

  test("timestamp query parameters (epoch-seconds format)") {
    val ts = Timestamp(1234567890L, 0)
    val queries = Queries(ts3 = Some(ts))
    val finished =
      Queries(ts3 = Some(ts), slm = Some(Map("ts3" -> "1234567890")))
    val expected = Metadata(query = Map("ts3" -> List("1234567890")))
    checkQueryRoundTrip(queries, expected, finished)
  }

  test("timestamp query parameters (http-date)") {
    val ts = Timestamp(1970, 1, 1, 0, 0, 0)
    val queries = Queries(ts4 = Some(ts))
    val finished = Queries(
      ts4 = Some(ts),
      slm = Some(Map("ts4" -> "Thu, 01 Jan 1970 00:00:00 GMT"))
    )
    val expected =
      Metadata(query = Map("ts4" -> List("Thu, 01 Jan 1970 00:00:00 GMT")))
    checkQueryRoundTrip(queries, expected, finished)
  }

  test("map of strings query param") {
    val map =
      Map("hello" -> "a", "world" -> "b")
    val queries = Queries(slm = Some(map))
    val expected = Metadata(query = map.fmap(Seq(_)))
    checkRoundTrip(queries, expected)
  }

  test("list of strings query param") {
    val list = List("hello", "world")
    val queries = Queries(sl = Some(list))
    val finished = Queries(sl = Some(list), slm = Some(Map("sl" -> "hello")))
    val expected = Metadata(query = Map("sl" -> list))
    checkQueryRoundTrip(queries, expected, finished)
  }

  test("Int Enum query parameter") {

    val queries = Queries(ie = Some(smithy4s.example.Numbers.ONE))
    val finished = Queries(
      ie = Some(smithy4s.example.Numbers.ONE),
      slm = Some(Map("nums" -> "1"))
    )
    val expected = Metadata(query = Map("nums" -> List("1")))
    checkQueryRoundTrip(queries, expected, finished)
  }

  // ///////////////////////////////////////////////////////////
  // HEADERS
  // ///////////////////////////////////////////////////////////
  test("String header") {
    val headers = Headers(str = Some("hello"))
    val expected = Metadata.empty.addHeader("str", "hello")
    checkRoundTrip(headers, expected)
  }

  test("String header with default") {
    val expected = HeadersWithDefaults(dflt = "test")
    checkRoundTripDefault(expected)
  }

  test("Integer header") {
    val headers = Headers(int = Some(123))
    val expected = Metadata.empty.addHeader("int", "123")
    checkRoundTrip(headers, expected)
  }

  test("Boolean header") {
    val headers = Headers(b = Some(true))
    val expected = Metadata.empty.addHeader("b", "true")
    checkRoundTrip(headers, expected)
  }

  test("timestamp header (not format)") {
    val ts = Timestamp(1970, 1, 1, 0, 0, 0)
    val headers = Headers(ts1 = Some(ts))
    val expected =
      Metadata.empty.addHeader("ts1", "Thu, 01 Jan 1970 00:00:00 GMT")
    checkRoundTrip(headers, expected)
  }

  test("timestamp header (date-time format)") {
    val ts = Timestamp(1970, 1, 1, 0, 0, 0)
    val headers = Headers(ts2 = Some(ts))
    val expected = Metadata.empty.addHeader("ts2", epochString)
    checkRoundTrip(headers, expected)
  }

  test("timestamp header (epoch-seconds format)") {
    val ts = Timestamp(1234567890L, 0)
    val headers = Headers(ts3 = Some(ts))
    val expected = Metadata.empty.addHeader("ts3", "1234567890")
    checkRoundTrip(headers, expected)
  }

  test("timestamp header (http-date)") {
    val ts = Timestamp(1970, 1, 1, 0, 0, 0)
    val headers = Headers(ts4 = Some(ts))
    val expected =
      Metadata.empty.addHeader("ts4", "Thu, 01 Jan 1970 00:00:00 GMT")
    checkRoundTrip(headers, expected)
  }

  test("map of strings header") {
    val map =
      Map(
        "hello" -> "a",
        "world" -> "b"
      )
    val expectedHeaders =
      Map(
        CaseInsensitive("foo-hello") -> List("a"),
        CaseInsensitive("foo-world") -> List("b")
      )
    val expected = Metadata(headers = expectedHeaders)
    val headers = Headers(slm = Some(map))
    checkRoundTrip(headers, expected)
  }

  test("list of strings header") {
    val list = List("hello", "world")
    val headers = Headers(sl = Some(list))
    val expected = Metadata.empty.addMultipleHeaders("sl", list)
    checkRoundTrip(headers, expected)
  }

  test("Int enum header") {
    val headers = Headers(ie = Some(smithy4s.example.Numbers.ONE))
    val expected = Metadata.empty.addHeader("nums", "1")
    checkRoundTrip(headers, expected)
  }

  // ///////////////////////////////////////////////////////////
  // PATH PARAMS
  // ///////////////////////////////////////////////////////////

  test("pathParams") {
    val ts = Timestamp(1970, 1, 1, 0, 0, 0)

    val pathParams = PathParams(
      str = "hello",
      int = 123,
      ts1 = ts,
      ts2 = ts,
      ts3 = Timestamp(1234567890L, 0),
      ts4 = ts,
      b = false,
      ie = smithy4s.example.Numbers.ONE
    )

    val expected =
      Metadata(path =
        Map(
          "str" -> "hello",
          "int" -> "123",
          "b" -> "false",
          "ts1" -> epochString,
          "ts2" -> epochString,
          "ts3" -> "1234567890",
          "ts4" -> "Thu, 01 Jan 1970 00:00:00 GMT",
          "ie" -> "1"
        )
      )
    checkRoundTrip(pathParams, expected)
  }

  // ///////////////////////////////////////////////////////////
  // Negatives
  // ///////////////////////////////////////////////////////////

  test("bad data gets caught") {
    val metadata =
      Metadata(query = Map("ts3" -> List("Thu, 01 Jan 1970 00:00:00 GMT")))
    val result = Metadata.decode[Queries](metadata)
    val expected = MetadataError.WrongType(
      "ts3",
      HttpBinding.QueryBinding("ts3"),
      "epoch-second timestamp",
      "Thu, 01 Jan 1970 00:00:00 GMT"
    )
    expect.same(result, Left(expected))
  }

  test("missing data gets caught") {
    val metadata = Metadata.empty
    val result = Metadata.decode[PathParams](metadata)
    val expected = MetadataError.NotFound(
      "str",
      HttpBinding.PathBinding("str")
    )
    expect.same(result, Left(expected))
  }

  test("too many parameters get caught") {
    val metadata =
      Metadata(query = Map("ts3" -> List("1", "2", "3")))
    val result = Metadata.decode[Queries](metadata)
    val expected = MetadataError.ArityError(
      "ts3",
      HttpBinding.QueryBinding("ts3")
    )
    expect.same(result, Left(expected))
  }

}
