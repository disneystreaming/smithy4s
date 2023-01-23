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

package smithy4s.compliancetests
package internals

import cats.implicits._
import ComplianceTest._
import cats.Eq
import io.circe.parser._
import org.http4s.Headers
import org.typelevel.ci.CIString
import smithy.test.{HttpRequestTestCase, HttpResponseTestCase}

private[internals] object assert {
  def success: ComplianceResult = Right(())
  def fail(msg: String): ComplianceResult = Left(msg)

  private def isJson(bodyMediaType: Option[String]) =
    bodyMediaType.exists(_.equalsIgnoreCase("application/json"))

  private def jsonEql(expected: String, actual: String): ComplianceResult = {
    (expected.isEmpty, actual.isEmpty) match {
      case (true, true)  => success
      case (true, false) => fail(s"Expected empty body, but got $actual")
      case (false, true) => fail(s"Expected $expected, but got empty body")
      case (false, false) =>
        (parse(expected), parse(actual)) match {
          case (Right(a), Right(b)) if a == b => success
          case (Left(a), Left(b)) => fail(s"Both JSONs are invalid: $a, $b")
          case (Left(a), _) =>
            fail(s"Expected JSON is invalid: $expected \n Error $a ")
          case (_, Left(b)) =>
            fail(s"Actual JSON is invalid: $actual \n Error $b")
          case (Right(a), Right(b)) =>
            fail(s"JSONs are not equal: expected json: $a \n actual json:  $b")
        }
    }
  }

  def eql[A: Eq](expected: A, actual: A): ComplianceResult = {
    if (expected === actual) {
      success
    } else {
      fail(
        s"Actual value: ${pprint.apply(actual)} was not equal to ${pprint.apply(expected)}."
      )
    }
  }

  def bodyEql(
      expected: String,
      actual: String,
      bodyMediaType: Option[String]
  ): ComplianceResult = {
    if (isJson(bodyMediaType)) {
      jsonEql(expected, actual)
    } else {
      eql(expected, actual)
    }
  }

  private def headersExistenceCheck(
      headers: Headers,
      expected: Either[Option[List[String]], Option[List[String]]]
  ) = {
    expected match {
      case Left(forbidHeaders) =>
        forbidHeaders.toList.flatten.collect {
          case key if headers.get(CIString(key)).isDefined =>
            assert.fail(s"Header $key is forbidden in the request.")
        }.combineAll
      case Right(requireHeaders) =>
        requireHeaders.toList.flatten.collect {
          case key if headers.get(CIString(key)).isEmpty =>
            assert.fail(s"Header $key is required request.")
        }.combineAll
    }
  }
  private def headersCheck(
      headers: Headers,
      expected: Option[Map[String, String]]
  ) = {
    expected.toList
      .flatMap(_.toList)
      .map { case (key, expectedValue) =>
        headers
          .get(CIString(key))
          .map { v =>
            assert.eql[String](expectedValue, v.head.value)
          }
          .getOrElse(
            assert.fail(s"'$key' header is missing")
          )
      }
      .combineAll
  }

  object testCase {
    def checkHeaders(
        tc: HttpRequestTestCase,
        headers: Headers
    ): ComplianceResult = {
      assert.headersExistenceCheck(headers, Left(tc.forbidHeaders)) *>
        assert.headersExistenceCheck(headers, Right(tc.requireHeaders)) *>
        assert.headersCheck(headers, tc.headers)
    }

    def checkHeaders(
        tc: HttpResponseTestCase,
        headers: Headers
    ): ComplianceResult = {
      assert.headersExistenceCheck(headers, Left(tc.forbidHeaders)) *>
        assert.headersExistenceCheck(headers, Right(tc.requireHeaders)) *>
        assert.headersCheck(headers, tc.headers)
    }
  }
}
