---
sidebar_label: Deriving CLIs
title: Deriving CLIs
---

- The Smithy4s Decline module provides the capability to derive a [Decline](https://ben.kirw.in/decline/) Cli for your service.
- The cli generated will be in the form of a [CommandIOApp](https://ben.kirw.in/decline/effect.html) 
- Let's revisit our HelloWorld smithy definition from the [Quickstart](../01-overview/02-quickstart.md)

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
 - Now Using the ```decline``` module from Smithy4s we can wrap the service instance in an instance of a `Smithy4sCli`.
    - The `Smithy4sCli` allows the customization of the Opts and stdin/stdout/stderr handling 
 - There is a convenient class ```Smithy4sSimpleStandaloneCli``` that you can extend and simply pass in the service wrapped in an Opts 
   - ``` object Hello extends standalone.Smithy4sSimpleStandaloneCli(Opts(HelloWorldServiceInstance.simple)) ```
 - ```Hello``` is now a runnable `CommandIOApp` and will provide the following interface

```
 Usage: hello-world-service hello [--output <output>] <name> [<town>]
 HTTP POST /{name}
 Options and flags:
     --help
         Display this help text.
     --output <output>
         Output mode
```

# Smithy to Decline mappings

   - The Service name will be used to generate the top level Command
     - All operations will be mapped to subcommands
   - The Input structure is flattened to top level fields with no nesting
     - All Primitives are mapped to a Positional `decline` Argument , with the following exceptions.
       - Boolean fields are mapped to a `decline` Flag
       - Blobs are mapped to take in either stdin(by passing in `-` ) or a full file path and will produce a Byte array 
       - Timestamps are parsed using TimestampFormat.DATE_TIME by default, but @timestampFormat can be used to customize that.
     - Lists and recursive types
       - a top level list is converted to a repeated positional argument (or flag in case of Boolean)
       - All recursive types expect json to be passed in.
     - How is Nesting handled
       - all nested fields are converted to Options 
       - a nested list is converted to expect a json i.e. List[String] would expect ```'["foo","baz","qux"]'``` as a decline Option
         - when the nested list contains blobs , it will expect a json of base64 encoded strings 
   - Help
     - Documentation is added to every field, operation and service - if available. For HTTP operations a path template is provided as well.

# Missing Features
   - Collision Handling for un-nesting of nested fields 
