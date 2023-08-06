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

import cats.effect._
import com.amazonaws.cloudwatch
import com.amazonaws.ec2
import org.http4s.client.middleware._
import org.http4s.ember.client.EmberClientBuilder
import smithy4s.aws._

object Main extends IOApp.Simple {

  override def run: IO[Unit] = awsEnvironmentResource.use { awsEnvironment =>
    // Per
    // https://disneystreaming.github.io/smithy4s/docs/protocols/aws/aws/#awsquery,
    // CloudWatch is one of a few services that use the awsQuery protocol.
    AwsClient(cloudwatch.CloudWatch, awsEnvironment).use(cloudWatchClient =>
      listAll[
        cloudwatch.NextToken,
        cloudwatch.ListMetricsOutput,
        cloudwatch.Metric
      ](
        listF = maybeNextToken =>
          cloudWatchClient.listMetrics(
            // This is just a simple way of reducing the size of the results while
            // still exercising the pagination handler.
            namespace = Some("AWS/S3"),
            nextToken = maybeNextToken
          ),
        accessResults = _.metrics.toList.flatten,
        accessNextToken = _.nextToken
      )
        .map(_.size)
        .flatMap(size => IO.println(s"Found $size metrics"))
    )
    // Per
    // https://disneystreaming.github.io/smithy4s/docs/protocols/aws/aws/#ec2query,
    // EC2 is the only service that use the ec2Query protocol.
    AwsClient(ec2.EC2, awsEnvironment).use(ec2Client =>
      listAll[String, ec2.DescribeInstanceStatusResult, ec2.InstanceStatus](
        listF = maybeNextToken =>
          ec2Client.describeInstanceStatus(
            maxResults = 100,
            nextToken = maybeNextToken
          ),
        accessResults = _.instanceStatuses.toList.flatten,
        accessNextToken = _.nextToken
      )
        .map(_.size)
        .flatMap(size => IO.println(s"Found $size instance statuses"))
    )
  }

  private val awsEnvironmentResource: Resource[IO, AwsEnvironment[IO]] =
    for {
      client <- EmberClientBuilder
        .default[IO]
        .build
        .map(
          RequestLogger.colored(
            logHeaders = true,
            logBody = true,
            logAction = Some(IO.println _)
          )
        )
      awsCredentialsProvider = new AwsCredentialsProvider[IO]
    } yield AwsEnvironment.make(
      client,
      IO.pure(AwsRegion.US_EAST_1),
      awsCredentialsProvider.defaultCredentialsFile.flatMap(
        awsCredentialsProvider
          .fromDisk(_, awsCredentialsProvider.getProfileFromEnv)
      ),
      IO.realTime.map(_.toSeconds).map(Timestamp(_, 0))
    )

  // This is probably something that's gonna get reimplemented a lot in
  // user-land. Perhaps we could use pagination hints from the specs to avoid
  // having to manually wire up the accessors, and to generate synthetic service
  // functions that handle pagination?
  private def listAll[NextToken, ListOutput, Result](
      listF: Option[NextToken] => IO[ListOutput],
      accessResults: ListOutput => List[Result],
      accessNextToken: ListOutput => Option[NextToken],
      acc: List[Result] = List.empty,
      maybeNextToken: Option[NextToken] = None
  ): IO[List[Result]] =
    for {
      listOutput <- listF(maybeNextToken)
      accumulatedResults = acc ::: accessResults(listOutput)
      result <- accessNextToken(listOutput) match {
        case None => IO.pure(accumulatedResults)
        case Some(nextNextToken) =>
          listAll(
            listF,
            accessResults,
            accessNextToken,
            accumulatedResults,
            Some(nextNextToken)
          )
      }
    } yield result

}
