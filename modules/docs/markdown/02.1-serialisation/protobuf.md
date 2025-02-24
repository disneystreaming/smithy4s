---
sidebar_label: Protobuf
title: Protobuf serialisation
---

The `smithy4s-protobuf` module provides [protocol-buffers](https://protobuf.dev/) codecs that can read/write generated data-types from protobuf-encoded bytes.

```
sbt : "com.disneystreaming.smithy4s" %% "smithy4s-protobuf" % "@VERSION@"
mill : "com.disneystreaming.smithy4s::smithy4s-protobuf:@VERSION@"
```

The entrypoint for Protobuf parsing/writing is `smithy4s.protobuf.Protobuf`. See below for example usage.

```scala mdoc:reset
import smithy4s.example.hello.Person
import smithy4s.protobuf.Protobuf

val personCodec = Protobuf.codecs.fromSchema(Person.schema)
val personBytes = personCodec.writeBlob(Person(name = "John Doe"))
val maybePerson = personCodec.readBlob(personBytes)
```

By default, `smithy4s-protobuf` abides by the semantics of :

* [alloy protobuf traits](https://github.com/disneystreaming/alloy/blob/main/docs/serialisation/protobuf.md). These semantics are the exact same semantics that [smithy-translate](https://github.com/disneystreaming/smithy-translate) uses to translate smithy to protobuf. This implies that the Smithy4s protobuf codecs are compatible with the codecs of other protobuf tools, generated from the .proto files resulting from running smithy through smithy-translate. In short, Smithy4s and [ScalaPB](https://github.com/scalapb/ScalaPB) can talk to each other : the ScalaPB codecs generated from protobuf after a translation from smithy are able to decode binary data produced by Smithy4s protobuf codecs (and vice versa).


```
┌────────────────────┐                        ┌────────────────────┐
│                    │                        │                    │
│                    │                        │                    │
│                    │                        │                    │
│                    │                        │                    │
│     Smithy IDL     ├────────────────────────►    Protobuf IDL    │
│                    │   smithy-translate     │                    │
│                    │                        │                    │
│                    │                        │                    │
│                    │                        │                    │
└─────────┬──────────┘                        └─────────┬──────────┘
          │                                             │
          │                                             │
          │                                             │
          │                                             │
          │                                             │
          │                                             │
          │ Smithy4s codegen                            │ ScalaPB codegen
          │                                             │
          │                                             │
          │                                             │
          │                                             │
          │                                             │
┌─────────▼──────────┐                        ┌─────────▼──────────┐
│                    │                        │                    │
│                    │                        │                    │
│                    │                        │                    │
│                    ◄────────────────────────┤                    │
│    Smithy4s code   │  Runtime communication │     ScalaPB code   │
│                    ├────────────────────────►                    │
│                    │                        │                    │
│                    │                        │                    │
│                    │                        │                    │
└────────────────────┘                        └────────────────────┘
```
