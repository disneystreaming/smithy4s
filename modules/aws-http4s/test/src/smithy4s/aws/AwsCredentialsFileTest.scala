/*
 *  Copyright 2021-2026 Disney Streaming
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

import smithy4s.aws.kernel.AwsCredentials
import weaver._

object AwsCredentialsFileTest extends FunSuite {
  val creds = AwsCredentials.Default("key", "sec", Some("token"))

  private def expectRight[A](
      e: Either[Throwable, A]
  )(f: A => Expectations): Expectations =
    e.fold(
      err => {
        err.printStackTrace()
        failure(s"Got Left but expected a Right. ${err.getMessage}")
      },
      f
    )

  test("find default profile") {
    expectRight(
      AwsCredentialsFile.processFileLines(
        asLines(
          """|[default]
             |aws_secret_access_key = sec
             |aws_access_key_id     = key
             |aws_session_token     = token""".stripMargin
        )
      )
    ) { res =>
      expect.same(Some(creds), res.default) &&
      expect.same(true, res.profiles.isEmpty)
    }
  }

  test("be case sensitive") {
    expectRight(
      AwsCredentialsFile.processFileLines(
        asLines(
          """|[default]
             |aws_secret_access_key = DeF_SeC
             |aws_access_key_id     = dEf_KEy
             |aws_session_token     = dEF_TokEn""".stripMargin
        )
      )
    ) { res =>
      expect.same(
        Some(AwsCredentials.Default("dEf_KEy", "DeF_SeC", Some("dEF_TokEn"))),
        res.default
      )
    }
  }

  test("parse comments") {
    expectRight(
      AwsCredentialsFile.processFileLines(
        asLines(
          """|# A comment
             |[default] # another comment
             |aws_secret_access_key = sec
             |aws_access_key_id     = key #yet another comment
             |aws_session_token     = token""".stripMargin
        )
      )
    ) { res =>
      expect.same(Some(creds), res.default)
    }
  }

  test("find some other profile") {
    expectRight(
      AwsCredentialsFile.processFileLines(
        asLines(
          """|[profile other]
             |aws_secret_access_key = sec
             |aws_access_key_id     = key
             |aws_session_token     = token""".stripMargin
        )
      )
    ) { res =>
      expect.same(None, res.default) &&
      expect.same(
        "other" -> creds,
        res.profiles.head
      )
    }
  }

  test("find some other profile w/o prefix") {
    expectRight(
      AwsCredentialsFile.processFileLines(
        asLines(
          """|[other]
             |aws_secret_access_key = sec
             |aws_access_key_id     = key
             |aws_session_token     = token""".stripMargin
        )
      )
    ) { res =>
      expect.same(None, res.default) &&
      expect.same(
        "other" -> creds,
        res.profiles.head
      )
    }
  }

  test("find default and some other profiles") {
    expectRight(
      AwsCredentialsFile.processFileLines(
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
    ) { res =>
      expect.same(Some(creds), res.default) &&
      expect.same(creds, res.profiles("p1")) &&
      expect.same(creds, res.profiles("p2"))
    }
  }

  test("profiles can have special characters in them") {
    expectRight(
      AwsCredentialsFile.processFileLines(
        asLines(
          """|[Namespace/User@USER_ID]
             |aws_access_key_id = key
             |aws_secret_access_key = sec
             |aws_session_token = token""".stripMargin
        )
      )
    ) { res =>
      expect.same(creds, res.profiles("Namespace/User@USER_ID"))
    }
  }

  test("values containing `=` are kept intact") {
    expectRight(
      AwsCredentialsFile.processFileLines(
        asLines(
          """|[default]
             |aws_secret_access_key = sec
             |aws_access_key_id     = key
             |aws_session_token     = dG9rZW4=a==""".stripMargin
        )
      )
    ) { res =>
      expect.same(
        Some(AwsCredentials.Default("key", "sec", Some("dG9rZW4=a=="))),
        res.default
      )
    }
  }

  test("the default profile can be selected by name") {
    expectRight(
      AwsCredentialsFile
        .processFileLines(
          asLines(
            """|[default]
               |aws_secret_access_key = sec
               |aws_access_key_id     = key
               |aws_session_token     = token""".stripMargin
          )
        )
        .flatMap(_.select(Some("default")))
    ) { res =>
      expect.same(creds, res)
    }
  }

  test("a line without `=` is reported with its line number") {
    val res = AwsCredentialsFile.processFileLines(
      asLines(
        """|[profile before]
           |aws_secret_access_key = sec
           |stray
           |aws_access_key_id     = key
           |[profile wanted]
           |aws_secret_access_key = sec
           |aws_access_key_id     = key""".stripMargin
      )
    )
    expect.same(
      Left(
        AwsCredentialsFileException(
          "Unable to parse credentials file at line 3: expected a `key = value` pair."
        )
      ),
      res
    )
  }

  test("a key-value pair before any profile header is rejected") {
    val res = AwsCredentialsFile.processFileLines(
      asLines(
        """|# header comment
           |aws_access_key_id = key
           |[default]
           |aws_secret_access_key = sec
           |aws_access_key_id     = key""".stripMargin
      )
    )
    expect.same(
      Left(
        AwsCredentialsFileException(
          "Unable to parse credentials file at line 2: expected a profile header like `[default]`."
        )
      ),
      res
    )
  }

  test("a profile missing its secret key is reported by name") {
    val res = AwsCredentialsFile.processFileLines(
      asLines(
        """|[profile p1]
           |aws_access_key_id = key""".stripMargin
      )
    )
    expect.same(
      Left(
        AwsCredentialsFileException(
          "'aws_secret_access_key' is missing from the data of profile `p1`."
        )
      ),
      res
    )
  }

  private def asLines(s: String) = s.split("\\n").toList
}
