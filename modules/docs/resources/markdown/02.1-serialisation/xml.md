---
sidebar_label: XML
title: XML serialisation
---

The `smithy4s-xml` module provides [fs2-data](https://fs2-data.gnieh.org/documentation/xml/) encoders/decoders that can read/write generated data-types from/to XML bytes/strings. It is provided at the following coordinates :

```
sbt : "com.disneystreaming.smithy4s" %% "smithy4s-xml" % "@VERSION@"
mill : "com.disneystreaming.smithy4s::smithy4s-xml:@VERSION@"
```

The entrypoint for  `smithy4s.xml.Xml`. See below for example usage.

```scala mdoc:reset
import smithy4s.example.hello.Person

import smithy4s.Blob
import smithy4s.xml.Xml

val personEncoder = Xml.encoders.fromSchema(Person.schema)
val personXML = personEncoder.encode(Person(name = "John Doe")).toUTF8String

val personDecoder = Xml.decoders.fromSchema(Person.schema)
val maybePerson = personDecoder.decode(Blob(personXML))
```

By default, `smithy4s-xml` abides by the semantics of :

* [official XML-related smithy traits](https://smithy.io/2.0/spec/protocol-traits.html#xml-bindings)

