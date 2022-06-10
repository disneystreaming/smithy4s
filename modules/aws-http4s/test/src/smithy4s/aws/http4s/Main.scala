/*
 *  Copyright 2021 Disney Streaming
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

package smithy4s.test

import cats.effect._
import com.amazonaws.dynamodb._
import com.amazonaws.lambda._
import org.http4s.ember.client.EmberClientBuilder
import smithy4s.aws._
import smithy4s.aws.http4s._
import cats.implicits._

object Main extends IOApp.Simple {

  def run = resource.use { case (dynamodb, lambda) =>
    dynamodb
      .describeTable(TableName("omelois-test"))
      .run
      .flatMap(IO.println(_)) *>
      lambda
        .listFunctions()
        .run
        .flatMap(IO.println(_))
        .whenA(
          false
        ) // FIXME: Lambda uses @restJson1 which is currently unsupported: https://github.com/disneystreaming/smithy4s/issues/53
  }

  val resource
      : Resource[IO, (AwsClient[DynamoDBGen, IO], AwsClient[LambdaGen, IO])] =
    for {
      httpClient <- EmberClientBuilder.default[IO].build
      dynamodb <- DynamoDB.awsClient(httpClient, AwsRegion.US_EAST_1)
      lambda <- Lambda.awsClient(httpClient, AwsRegion.US_EAST_1)
    } yield (dynamodb, lambda)

}
