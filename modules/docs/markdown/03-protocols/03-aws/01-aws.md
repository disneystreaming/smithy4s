---
sidebar_label: AWS
---

**WARNING: READ THE FOLLOWING, AND USE WITH CAUTION**

Smithy4s provides functions to create AWS clients from generated code. As of 0.18, Smithy4s supports (at least partially) all AWS protocols that are publicly documented.

Our implementation of the AWS protocols is tested against the official [compliance-tests](https://github.com/smithy-lang/smithy/tree/main/smithy-aws-protocol-tests/model), which gives us a reasonable level of confidence that most of the (de)serialisation logic is correct involved when communicating with AWS is correct. Our implementation of the AWS signature algorithm.
(which allows AWS to authenticate requests) is tested against the Java implementation used by the official AWS SDK.

### What is missing ?

* streaming operations (such as S3 `putObject`, `getObject`, or Kinesis' `subscribeToShard`) are currently unsupported.
* [service-specific customisations](https://smithy.io/2.0/aws/customizations/index.html)  are currently unsupported.
* **users should not use smithy4s to talk to AWS S3**

### Where to find the specs ?

* SBT : `"com.disneystreaming.smithy" % s"aws-${service_name}-spec" % "@AWS_SPEC_VERSION@"`
* Mill : `ivy"com.disneystreaming.smithy:aws-${service_name}-spec:@AWS_SPEC_VERSION@"`

The version corresponds to the latest release in this repo: [aws-sdk-smithy-specs](https://github.com/disneystreaming/aws-sdk-smithy-specs).

AWS does not publishes the specs to their services to Maven. However, The specs in question (that are written in json syntax) can be found in some of the [official SDKs](https://github.com/aws/aws-sdk-js-v3/tree/main/codegen/sdk-codegen/aws-models) published by AWS. These `.json files` can be understood by smithy4s, just like `.smithy`, and can be used to generate code.

The **aws-sdk-smithy-specs** project periodically gathers the specs from the Javascript SDK repo and publishes them
to maven central to lower the barrier of entry.

### Note on pre-built artifacts

We (the Smithy4s maintainers) **do not** intend to publish pre-generated artifacts containing the AWS clients, there's a lot of nuance there and maintainance burden that we do not have the capacity to assume. In particular, backward binary compatibility of the generated code is impossible to guarantee at this time.

## Setup

### SBT

In `build.sbt`

```scala
import smithy4s.codegen.BuildInfo._

libraryDependencies ++= Seq(
  // version sourced from the plugin
  "com.disneystreaming.smithy4s" %% "smithy4s-aws-http4s" % smithy4sVersion.value
  "com.disneystreaming.smithy" % "aws-dynamodb-spec" % "@AWS_SPEC_VERSION@" % Smithy4s
)
```

## Example usage

In your Scala code:

```scala mdoc:compile-only
import cats.effect._
import org.http4s.ember.client.EmberClientBuilder

import smithy4s.aws._ // AWS specific interpreters
import com.amazonaws.dynamodb._ // Generated code from specs.

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

```

## Service summary

Below you'll find a generated summary of the maven coordinates for the AWS specifications. Note
that the version of the spec might not be the latest one. Refer yourself to [this repo](https://github.com/disneystreaming/aws-sdk-smithy-specs) to get the latest version of the specs.

```scala mdoc:passthrough
docs.AwsServiceList.renderServiceList()
```
