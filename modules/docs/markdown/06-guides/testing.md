---
sidebar_label: Testing
title: Testing Smithy4s Applications
---

In this guide, we will give you some guiding principles and other things to consider when testing Smithy4s applications.

## What Smithy4s Does

For starters, let's briefly cover what it is that Smithy4s does. At a high level, Smithy4s takes your Smithy model and generates Scala code based on that model. The model is essentially just a Scala representation of the Smithy model. The generated code _does not_ contain any logic for any specific library such as http4s or jsoniter. Instead, it contains abstractions that Smithy4s itself defines that interpreters can be based on. This means that the generated code is not coupled to any library (other than Smithy4s itself). The interpreters that we have created in Smithy4s will then take the generated code and use it to power, for example, an HTTP service that uses http4s and jsoniter.

## How we Test Smithy4s

It is important to understand how Smithy4s works so you can begin to conceptualize how we test Smithy4s and its interpreters. We will not go into much detail on this point, but the main ways we test these are through:

- Unit tests on the interpreters. These tests isolate the interpreters directly and test their functionality. An example of this is the Jsoniter interpreter. We are able to isolate its functionality and test that it is working as intended.
- Protocol tests. These tests are auto-generated from Smithy-defined traits. They are sort of like integration tests in the sense that they test all the relevant layers of a smithy4s application by connecting, for example, the HTTP layer with the JSON serialization layer. The main goal of these tests is to make sure that Smithy4s is handling edge cases properly and overall conforms to the protocol(s) we have defined. For more on protocol testing, you can check out the [Smithy documentation](https://smithy.io/2.0/additional-specs/http-protocol-compliance-tests.html).

## Testing your API

Now the question that remains is what all of this means for your applications which use Smithy4s. Our basic recommendation is going to come down to: test what you feel needs to be tested, but make sure you understand _what_ it is that you are testing.

For example, we would not generally recommend that you test the Smithy4s interpreters in your code (for example the http4s routes that you get when you use `SimpleRestJsonBuilder`). One of the main appeals of Smithy4s is that it abstracts away concerns such as request routing and the encoding/decoding of payloads. This is why we strive to be thorough in our testing: so you don't need to worry about testing whether or not Smithy4s is doing its job. That being said, if you don't trust Smithy4s and want to test it anyway, that is totally fine too. Ultimately Smithy4s as a project will benefit if you do test it and find any bugs that we can then fix. So in any case, we are supportive of your decision.

Another reason you may think of testing the Smithy4s interpreters is to make sure that their implementation, such as the way they encode something in JSON, lines up with some requirements that you have. This is fair enough, but realize that the thing you are really testing here is your understanding of how Smithy4s works. That may be worth your while, or it may not be. The decision is ultimately up to you. If this is your reason for testing, you may want to explore contract-based testing as outlined below.

Note that if you are going to test Smithy4s interpreter behaviors, you should _not_ use Smithy4s to test them. We have occasionally seen the pattern where someone uses a Smithy4s client to test a Smithy4s server. This type of testing has very little benefit since much of the same logic is used for each of these interpreters. This means that the interpreters you are testing could both have the same bug and your tests would not catch it.

On the other hand, we do recommend that you test the implementation of your service. For example, if Smithy4s generates a service interface called `HelloWorldService`, you will implement that interface and place your business logic within it. We recommend that you test all of your business logic since this is outside the scope of what Smithy4s controls.

## Schema and Contract Testing

Contract testing is a form of testing that checks that the contracts which services and clients agree upon are stable over time. Note that schema-based tests and contract-based test are not the same thing. Schema tests are concerned with checking that the data sent and received conforms to a specified schema or schemas. An example where contract testing differs is, for example, a client may expect a certain error response to be returned whenever certain conditions are met in the request. A schema can capture what errors may possibly be sent, but it will not capture which error should be triggered by a specific request.

For your specific use case, you may have a need to employ schema-based and/or contract-based testing. Schema based testing would be one way of checking that schema changes over time do not impact existing clients. Contract-based testing could accomplish this same thing and more. However, contract based testing is usually more difficult to implement than schema-based testing so you may find it not to be worth it for your use case. In either case, remember to make sure you know _what_ it is you are testing and what you are not.

## TL;DR

Ultimately our recommendation is that you unit test all of your business logic as well as any custom implementations you have created on top of Smithy4s. You should be able to leave the rest of the unit testing up to us. However, your requirements, concerns, and experience may vary and we encourage you to do whatever will help you write the best software.

Depending on your use case, you may find value in Schema or Contract testing. These methods can help you make sure your API is stable for consumers over time.
