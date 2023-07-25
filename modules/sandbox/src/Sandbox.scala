import smithy4s.aws._
import cats.effect._
import org.http4s.client.middleware._
import org.http4s.ember.client.EmberClientBuilder

import com.amazonaws.cloudwatch._

object Main extends IOApp.Simple {

  type NextToken = String

  override def run: IO[Unit] = cloudWatchClientResource.use(cloudWatchClient =>
    listAll[ListMetricsOutput, Metric](
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

  private val cloudWatchClientResource: Resource[IO, CloudWatch[IO]] =
    for {
      httpClient <- EmberClientBuilder
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
      awsEnv = AwsEnvironment.make(
        httpClient,
        IO.pure(AwsRegion.US_EAST_1),
        awsCredentialsProvider.defaultCredentialsFile.flatMap(
          awsCredentialsProvider
            .fromDisk(_, awsCredentialsProvider.getProfileFromEnv)
        ),
        IO.realTime.map(_.toSeconds).map(Timestamp(_, 0))
      )
      cloudWatch <- AwsClient(CloudWatch, awsEnv)
    } yield cloudWatch

  // This is probably something that's gonna get reimplemented a lot in
  // user-land. Perhaps we could use pagination hints from the specs to avoid
  // having to manually wire up the accessors, and to generate synthetic service
  // functions that handle pagination?
  private def listAll[ListOutput, Result](
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
