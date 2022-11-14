---
sidebar_label: AWS
---

**WARNING: THIS IS EXPERIMENTAL, DO NOT NOT EXPECT PRODUCTION READINESS**

Smithy4s provides functions to create AWS clients from generated code. At the time of writing this, smithy4s is only able to derive clients for AWS services that use the [AWS Json 1.0/1.1 protocol](https://awslabs.github.io/smithy/1.0/spec/aws/index.html?highlight=aws%20protocols#aws-protocols).

The AWS smithy specs (that are written in json syntax) can be found in some of the [official SDKs](https://github.com/aws/aws-sdk-js-v3/tree/main/codegen/sdk-codegen/aws-models) published by AWS. These `.json files` can be understood by smithy4s, just like `.smithy`, and can be used to generate code. Just copy/paste them in your project.

We (the Smithy4s maintainers) **do not** intend to publish pre-generated artifacts containing the AWS clients, there's a lot of nuance there and maintainance burden that we do not have the capacity to assume. In particular, backward binary compatibility of the generated code is impossible to guarantee at this time.

#### Setup

In `build.sbt`

```scala
import smithy4s.codegen.BuildInfo._

libraryDependencies ++= Seq(
  // contains traits used by specs of AWS services"
  "software.amazon.smithy" % "smithy-aws-traits" % smithyVersion % Smithy4s,
  "software.amazon.smithy" % "smithy-waiters" % smithyVersion % Smithy4s,
  // version sourced from the plugin
  "com.disneystreaming.smithy4s"  %% "smithy4s-aws-http4s" % smithy4sVersion.value
)
```

In your Scala code:

```scala mdoc:compile-only
import cats.effect._
import org.http4s.ember.client.EmberClientBuilder

import smithy4s.aws._ // AWS models and cats-effect/fs2 specific functions
import smithy4s.aws.http4s._ // AWS/http4s specific integration
import com.amazonaws.dynamodb._ // Generated code from specs.

object Main extends IOApp.Simple {

  def run = resource.use { dynamodb =>
    dynamodb
      .describeTable(TableName("omelois-test"))
      .run
      .flatMap(IO.println(_))
  }

  val resource: Resource[IO, AwsClient[DynamoDBGen, IO]] = for {
    httpClient <- EmberClientBuilder.default[IO].build
    dynamodb <- DynamoDB.awsClient(httpClient, AwsRegion.US_EAST_1)
  } yield dynamodb
}

```
