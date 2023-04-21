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

import cats.effect._
import com.amazonaws.dynamodb._
import org.http4s.ember.client.EmberClientBuilder
import smithy4s.aws._

object Main extends IOApp.Simple {

  def run = resource.use { case (dynamodb) =>
    dynamodb
      .listTables(limit = Some(ListTablesInputLimit(10)))
      .flatMap(IO.println(_))
  }

  val resource: Resource[IO, DynamoDB[IO]] =
    for {
      httpClient <- EmberClientBuilder.default[IO].build
      awsEnv <- AwsEnvironment.default(httpClient, AwsRegion.US_EAST_1)
      dynamodb <- AwsClient(DynamoDB, awsEnv)
    } yield dynamodb

}