---
sidebar_label: Deriving CLIs
title: Deriving CLIs
---

- The Smithy4s Decline module provides the capability to derive a [Decline](https://ben.kirw.in/decline/) Cli for your service.
- The cli generated will be in the form of a [Command[F[Unit]]](https://ben.kirw.in/decline/usage.html#commands-and-subcommands) where F is the effect type of your service.
- This module is written in [Tagless Final](https://okmij.org/ftp/tagless-final/) style and requires an F[] for which there is an instance of [cats.MonadThrow](https://typelevel.org/cats/api/cats/package$$MonadThrow$.html)  
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

Lets implement the HelloWorld service , we will use cats.effect.IO for our effect type.

```scala mdoc:silent
import smithy4s.hello._
import cats.effect.IO
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
 - There is a convenient helper method ```Smithy4sCli.standalone``` to make it easier to construct Cli's using  defaults.
 - To utilize the helper method, wrap the service in an instance of `Opts` and pass it into ```Smithy4sCli.standalone```
 - there are 2 methods available on the ```Smithy4sCli``` instance
    - ```opts``` which will provide an `Opts[F[Unit]]` for the service
    - ```command``` which will provide a `Command[F[Unit]]` for the service. This uses defaults from the Smithy spec
      - command name will use the service name 
      - if documentation comments are available on the service , they will be used as the command help text
```scala mdoc:silent
   import com.monovore.decline._
   import smithy4s.decline.Smithy4sCli
   val serviceWrappedInOpts = Opts(HelloWorldServiceInstance.simple)
   val helloCommand: Command[IO[Unit]] = Smithy4sCli.standalone(serviceWrappedInOpts).command
```   
 - ```helloCommand``` is now a runnable `Command` that can parse command line args and returns an IO[Unit]
 - We can implement a CLI that will run the command and print the result to stdout
```scala mdoc:silent

import smithy4s.decline.Smithy4sCli
import cats.effect._
import com.monovore.decline._
import com.monovore.decline.effect.CommandIOApp

object app extends IOApp {
  override def run(args: List[String]) = {
    val helloCommand: Command[IO[ExitCode]] = Smithy4sCli
      .standalone(Opts(HelloWorldServiceInstance.simple))
      .command.map(_.redeem(_ => ExitCode.Error, _ => ExitCode.Success))
    CommandIOApp.run(helloCommand, args)
  }
}
```
- the command will  provide the following interface 
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
