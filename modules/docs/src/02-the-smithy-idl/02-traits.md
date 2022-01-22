---
sidebar_label: Smithy traits
title: Smithy traits
---

Smithy comes with a powerful annotation system. Annotations in smithy are called [traits](https://awslabs.github.io/smithy/1.0/spec/core/model.html#applying-traits-to-shapes).

These traits let you associate protocol-specific details to your data models and services.

For instance, an operation can be labelled as compatible with http semantics using the [http traits](https://awslabs.github.io/smithy/1.0/spec/core/http-traits.html?highlight=http%20traits#http-binding-traits):

```kotlin
namespace foo

@http(method: "GET", uri: "/hello/{name}")
operation Greet {
  input: GreetInput,
  output: GreetOutput,
  errors: [BadInput]
}

structure GreetInput {
  // Matches the {name} hole in the uri path above
  @httpLabel
  name: String
}

structure GreetOutput {
  @httpPayload
  message: String
}

@error("client")
@httpError(400)
structure BadInput {
  @jsonName("oops")
  message: String
}
```

### Creating your own traits

Smithy makes it really easy to create your own traits:

```kotlin
namespace foo

@trait(selector: is(structure))
string customThing

@customThing("hello")
structure MyStructure {
}
```

### Regarding smithy4s handling of traits

Smithy4s automatically creates corresponding values in the generated Scala code, for all the annotations it finds, whether defined in the smithy prelude, or defined by users.

These values can be retrieved via some interfaces that will be documented in a near future.
