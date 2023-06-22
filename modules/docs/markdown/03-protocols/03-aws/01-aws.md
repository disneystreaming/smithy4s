---
sidebar_label: AWS
---

**WARNING: THIS IS EXPERIMENTAL, DO NOT NOT EXPECT PRODUCTION READINESS**

Smithy4s provides functions to create AWS clients from generated code. At the time of writing this, smithy4s is only able to derive clients for AWS services.

At the time of writing this, Smithy4s supports a subset of the [protocols](https://awslabs.github.io/smithy/1.0/spec/aws/index.html?highlight=aws%20protocols#aws-protocols) that AWS uses in their services.

The supported protocols are :

* AWS Json 1.0
* AWS Json 1.1

### Where to find the specs ?

* SBT : `"com.disneystreaming.smithy" % s"aws-${service_name}-spec" % "@AWS_SPEC_VERSION@"`
* Mill : `ivy"com.disneystreaming.smithy:aws-${service_name}-spec:@AWS_SPEC_VERSION@"`

The version corresponds tho the latest release in this repo: [aws-sdk-smithy-specs](https://github.com/disneystreaming/aws-sdk-smithy-specs).

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

import smithy4s.aws._ // AWS models and cats-effect/fs2 specific functions
import smithy4s.aws.http4s._ // AWS/http4s specific integration
import com.amazonaws.dynamodb._ // Generated code from specs.

object Main extends IOApp.Simple {

  def run = resource.use { dynamodb =>
    dynamodb
      .describeTable(TableName("omelois-test"))
      .flatMap(IO.println(_))
  }

  val resource: Resource[IO, DynamoDB[IO]] = for {
    httpClient <- EmberClientBuilder.default[IO].build
    dynamodb <- DynamoDB.simpleAwsClient(httpClient, AwsRegion.US_EAST_1)
  } yield dynamodb
}

```

## Service summary

Below you'll find a generated summary of the maven coordinates for the AWS specifications. Note
that the version of the spec might not be the latest one. Refer yourself to [this repo](https://github.com/disneystreaming/aws-sdk-smithy-specs) to get the latest version of the specs.

```scala mdoc:passthrough
docs.AwsServiceList.renderServiceList()
```
