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
import fs2.io.file.Files
import smithy4s.aws.kernel.AwsCredentials
import cats.effect.IO

object AwsCredentialsFileReadTest extends SimpleIOSuite {
  test("read credentials from a file") {
    Files[IO].tempFile.use { path =>
      val content = """|[default]
                       |aws_secret_access_key = def_sec
                       |aws_access_key_id     = def_key
                       |aws_session_token     = def_token
                       |[profile p1]
                       |aws_secret_access_key = sec
                       |aws_access_key_id     = key
                       |aws_session_token     = token
                  """.stripMargin

      val writeFile = fs2.Stream
        .emit(content)
        .through(fs2.text.utf8.encode)
        .through(Files[IO].writeAll(path))
        .compile
        .drain
      val readDefault = AwsCredentialsFile.fromDisk(path, None)
      val readP1 = AwsCredentialsFile.fromDisk(path, Some("p1"))

      for {
        _ <- writeFile

        default <- readDefault
        p1 <- readP1
      } yield {
        val expectedP1 =
          AwsCredentials.Default("key", "sec", Some("token"))
        val expectedDefault =
          AwsCredentials.Default("def_key", "def_sec", Some("def_token"))
        expect.same(expectedDefault, default) &&
        expect.same(expectedP1, p1)
      }
    }
  }
}
