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

package smithy4s.http4s

import weaver._

import org.http4s.HttpApp
import org.http4s.client.Client
import cats.effect.IO

// This is a non-regression test for https://github.com/disneystreaming/smithy4s/issues/181
object RecursiveInputSpec extends FunSuite {

  test("restJson works with recursive input operations") {
    val result =
      RestJsonBuilder(smithy4s.example.RecursiveInputService)
        .client(Client.fromHttpApp(HttpApp.notFound[IO]))
        .use

    expect(result.isRight)
  }

}
