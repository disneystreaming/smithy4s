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

package smithy4s.aws

import weaver._

object AwsCredentialsFileTest extends FunSuite {
  val creds = AwsCredentials.Default("key", "sec", Some("token"))

  test("find default profile") {
    val res = AwsCredentialsFile.processFileLines(
      asLines(
        """|[default]
           |aws_secret_access_key = sec
           |aws_access_key_id     = key
           |aws_session_token     = token""".stripMargin
      )
    )
    expect.same(res.default, Some(creds)) &&
    expect.same(true, res.profiles.isEmpty)
  }

  test("find some other profile") {
    val res = AwsCredentialsFile.processFileLines(
      asLines(
        """|[profile other]
           |aws_secret_access_key = sec
           |aws_access_key_id     = key
           |aws_session_token     = token""".stripMargin
      )
    )
    expect.same(res.default, None) &&
    expect.same(
      res.profiles.head,
      "other" -> creds
    )
  }

  test("find some other profile w/o prefix") {
    val res = AwsCredentialsFile.processFileLines(
      asLines(
        """|[other]
           |aws_secret_access_key = sec
           |aws_access_key_id     = key
           |aws_session_token     = token""".stripMargin
      )
    )
    expect.same(res.default, None) &&
    expect.same(
      res.profiles.head,
      "other" -> creds
    )
  }

  test("find default and some other profiles") {
    val res = AwsCredentialsFile.processFileLines(
      asLines(
        """|[default]
           |aws_secret_access_key = sec
           |aws_access_key_id     = key
           |aws_session_token     = token
           |[profile p1]
           |aws_secret_access_key = sec
           |aws_access_key_id     = key
           |aws_session_token     = token
           |
           |[p2]
           |aws_secret_access_key = sec
           |aws_access_key_id     = key
           |aws_session_token     = token""".stripMargin
      )
    )
    expect.same(res.default, Some(creds)) &&
    expect.same(res.profiles("p1"), creds) &&
    expect.same(res.profiles("p2"), creds)
  }

  private def asLines(s: String) = s.split("\\n").toList
}
