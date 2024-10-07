---
sidebar_label: SimpleRestJson
title: The SimpleRestJson protocol
---

Smithy4s provides a custom Json-in/Json-out protocol that smithy services can be annotated with.

Smithy4s comes with opt-in http4s-specific module, that contains functions that are aware of this protocol, and can be used to quickly derive http services and clients.

As for the json aspect of the protocol, [jsoniter-scala](https://github.com/plokhotnyuk/jsoniter-scala/) is used for the (de)serialisation of the http bodies.

## Semantics

In this protocol, the values in shapes are bound to http metadata or body according to the specification of the [Http Binding traits](https://awslabs.github.io/smithy/1.0/spec/core/http-traits.html?highlight=http#http-binding-traits). However, the `@mediaType` trait has no incidence, and all bodies (when present) are serialised in JSON.

## Example spec

```kotlin
namespace smithy4s.example

use alloy#simpleRestJson

@simpleRestJson
service HelloWorldService {
  version: "1.0.0"
  // Indicates that all operations in `HelloWorldService`,
  // here limited to the Hello operation, can return `GenericServerError`.
  errors: [GenericServerError]
  operations: [Hello]
}

@error("server")
@httpError(500)
structure GenericServerError {
  message: String
}

@http(method: "POST", uri: "/{name}", code: 200)
operation Hello {
  input: Person
  output: Greeting
}

structure Person {
  @httpLabel
  @required
  name: String

  @httpQuery("town")
  town: String
}

structure Greeting {
  @required
  message: String
}
```

## Supported traits

This protocol and its interpreters, are aware of the following traits provided out of the box:

* [all simple shapes](https://awslabs.github.io/smithy/1.0/spec/core/model.html#simple-shapes)
* composite data shapes, including collections, unions, structures.
* [operations and services](https://awslabs.github.io/smithy/1.0/spec/core/model.html#service)
* [enumerations](https://awslabs.github.io/smithy/1.0/spec/core/constraint-traits.html#enum-trait)
* [error trait](https://awslabs.github.io/smithy/1.0/spec/core/type-refinement-traits.html#error-trait)
* [http traits](https://awslabs.github.io/smithy/1.0/spec/core/http-traits.html)
* [timestampFormat trait](https://awslabs.github.io/smithy/1.0/spec/core/protocol-traits.html?highlight=timestampformat#timestampformat-trait)

For the full list, see below.

## Decoding and encoding unions

The `SimpleRestJson` protocol supports 3 different union encodings :

* tagged (default)
* untagged
* discriminated

See the section about [unions](../../04-codegen/02-unions.md) for a detailed description.

## Json Array Arity

* By default there is a limit on the arity of an array, which is 1024. This is to prevent the server from being overloaded with a large array as this is a vector for attacks.
* This limit can be changed by setting the maxArity `smithy4s.http4s.SimpleRestJsonBuilder.withMaxArity(.)` to the desired value.
* an example can be seen in the [client example](03-client.md)

## Explicit Null Encoding

By default, optional properties (headers, query parameters, structure fields) that are set to `None` and optional properties that are set to default value will be excluded during encoding process. If you wish to change this so that instead they are included and set to `null` explicitly, you can do so by calling `.withExplicitDefaultsEncoding(true)`.

## Other customisations of JSON codec behaviour

The underlying JSON codecs can be configured with a number of options to cater to niche usecases, via the `.transformJsonCodecs` method, which takes a function that takes in and returns a
`JsonPayloadCodecCompiler`. For instance, by default, the `NaN` and `Infinity` values are not considered valid during parsing `Float` or `Double` values. This can be amended via
`.transformJsonCodecs(_.configureJsoniterCodecCompiler(_.withInfinitySupport(true)))`.

The customisations are bound to evolve as we uncover new niche cases that warrant adding new pieces of opt-in behaviour. The default behaviour is kept rather strict as it helps keep competitive performance and safety.

## Supported traits

Here is the list of traits supported by `SimpleRestJson`

```scala mdoc:passthrough
alloy.SimpleRestJson.hints
  .get[smithy.api.ProtocolDefinition]
  .getOrElse(sys.error("Unable to grab protocol defition information."))
  .traits.toList.flatten.map(_.value)
  .map(id => s"- `$id`")
  .foreach(println)
```

Currently, `@cors` is not supported. This is because the `@cors` annotation is too restrictive. You can still use it in your model and configure your API using the information found in the generated code. See the [`Cors.scala`](@GITHUB_BRANCH_URL@modules/guides/src/smithy4s/guides/Cors.scala) file in the `guides` module for an example.

## Structured Strings

As of smithy4s version `0.18.x`, you are able to create strings which are parsed directly into structures for you. This can be accomplished using the `alloy#structurePattern` trait. For example:

```smithy
@structurePattern(pattern: "{foo}_{bar}", target: FooBar)
string FooBarString

structure FooBar {
  @required
  foo: String
  @required
  bar: Integer
}
```

Now wherever `FooBarString` is used, it will really be parsing the string into the structure `FooBar`. As such, the generated code will replace instances of `FooBarString` with `FooBar` such that the parsing logic is abstracted away from your implementation. See the [alloy documentation](https://github.com/disneystreaming/alloy#alloystructurepattern) for more information.
