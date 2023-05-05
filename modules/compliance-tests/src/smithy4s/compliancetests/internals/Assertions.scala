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
import io.circe.Json
import org.http4s.Headers
import org.typelevel.ci.CIString
import smithy.test.{HttpRequestTestCase, HttpResponseTestCase}
import io.circe.parser._

private[internals] object assert {
  def success: ComplianceResult = Right(())
  def fail(msg: String): ComplianceResult = Left(msg)

  private def isJson(bodyMediaType: Option[String]) =
    bodyMediaType.forall(_.equalsIgnoreCase("application/json"))

  private def jsonEql(result: String, testCase: String): ComplianceResult = {
    (result.isEmpty, testCase.isEmpty) match {
      case (true, true) => success
      case _ =>
        val nonEmpty = if (result.isEmpty) "{}" else result
        (parse(result), parse(nonEmpty)) match {
          case (Right(a), Right(b)) if Eq[Json].eqv(a, b) => success
          case (Left(a), Left(b)) => fail(s"Both JSONs are invalid: $a, $b")
          case (Left(a), _) =>
            fail(s"Result JSON is invalid: $result \n Error $a ")
          case (_, Left(b)) =>
            fail(s"TestCase JSON is invalid: $testCase \n Error $b")
          case (Right(a), Right(b)) =>
            fail(s"JSONs are not equal: result json: $a \n testcase json:  $b")
        }
    }
  }

  def eql[A: Eq](
      result: A,
      testCase: A,
      prefix: String = ""
  ): ComplianceResult = {
    if (result === testCase) {
      success
    } else {
      fail(
        s"$prefix the result value: ${pprint.apply(result)} was not equal to the expected TestCase value ${pprint
          .apply(testCase)}."
      )
    }
  }

  def bodyEql(
      result: String,
      testCase: String,
      bodyMediaType: Option[String]
  ): ComplianceResult = {
    if (isJson(bodyMediaType)) {
      jsonEql(result, testCase)
    } else {
      eql(result, testCase)
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
            assert.eql[String](v.head.value, expectedValue, s"Header $key: ")
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
