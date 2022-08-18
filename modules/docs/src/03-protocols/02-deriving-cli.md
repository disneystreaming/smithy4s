---
sidebar_label: Deriving CLIs
title: Deriving CLIs
---

- The Smithy4s-Cli module provide the capability to derive a [Decline](https://ben.kirw.in/decline/) cli for your service.
- Lets revisit our HelloWorld smithy definition from the Quickstart

```kotlin
namespace smithy4s.hello

use smithy4s.api#simpleRestJson

@simpleRestJson
service HelloWorldService {
  version: "1.0.0",
  operations: [Hello]
}

@http(method: "POST", uri: "/{name}", code: 200)
operation Hello {
  input: Person,
  output: Greeting
}

structure Person {
  @httpLabel
  @required
  name: String,

  @httpQuery("town")
  town: String
}

structure Greeting {
  @required
  message: String
}
```

Lets implement the HelloWorld service
```scala
object HelloWorldServiceInstance{
  val simple = new HelloWorldService[IO]{
    def hello(name: String, town: Option[String]):IO[Greeting] = {
      IO{
        Greeting(s"hello $name , who hails from $town")
      }
    }
  }
}
```
 - Now Using the ```cli``` module from smithy4s we can wrap the service instance in an instance of Smithy4sCli.
 - There is a convenient class ```Smithy4sSimpleStandaloneCli``` that you can extend and simply pass in the service wrapped in an Opts 
   - ``` object Hello extends standalone.Smithy4sSimpleStandaloneCli(Opts(HelloWorldServiceInstance.simple)) ```
 - ```Hello``` is now a runnable CommandIOApp and will provide the following interface

```
 Usage: hello-world-service hello [--output <output>] <name> [<town>]
 HTTP POST /{name}
 Options and flags:
     --help
         Display this help text.
     --output <output>
         Output mode
```

