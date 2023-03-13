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
import smithy4s.example.aws.MyThing

object ClientPrepareTest extends FunSuite {
  test(
    "Using a service without a supported protocol gives you a list of supported ones"
  ) {
    AwsClient.prepare(MyThing) match {
      case Left(p) =>
        assert.same(
          p,
          AwsClientInitialisationError.UnsupportedProtocol(
            MyThing.id,
            List(
              aws.protocols.AwsJson1_0.getTag,
              aws.protocols.AwsJson1_1.getTag,
              aws.protocols.AwsQuery.getTag
            )
          )
        )

      case Right(other) =>
        failure(s"Expected Left, got Right instead: $other")
    }
  }
}
