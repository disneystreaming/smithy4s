---
sidebar_label: JSON
title: JSON serialisation
---

The `smithy4s-json` module provides [jsoniter-based](https://github.com/plokhotnyuk/jsoniter-scala) encoders/decoders that can read/write generated data-types from/to JSON bytes/strings, without an intermediate JSON ADT. The performance of this module is [very competitive](https://plokhotnyuk.github.io/jsoniter-scala/) in the Scala ecosystem.

This module is provided at the following coordinates :

```
sbt : "com.disneystreaming.smithy4s" %% "smithy4s-json" % "@VERSION@"
mill : "com.disneystreaming.smithy4s::smithy4s-json:@VERSION@"
```

The entrypoint for JSON parsing/writing is `smithy4s.json.Json`. See below for example usage.

> Note: If the `Schema` is non-static, such as a dynamically generated one, then reading and writing Json may result in memory leaks.

```scala mdoc:reset
import smithy4s.example.hello.Person

import smithy4s.Blob
import smithy4s.json.Json

val person = Person(name = "John Doe")

val personEncoder = Json.payloadCodecs.encoders.fromSchema(Person.schema)
val personJSON = personEncoder.encode(person).toUTF8String
val personJSON2 = Json.writePrettyString(person)

val personDecoder = Json.payloadCodecs.decoders.fromSchema(Person.schema)
val maybePerson = personDecoder.decode(Blob(personJSON))
val maybePerson2 = Json.read[Person](Blob(personJSON2))
```

By default, `smithy4s-json` abides by the semantics of :

* [official smithy traits](https://smithy.io/2.0/spec/protocol-traits.html), including:
  * [jsonName](https://smithy.io/2.0/spec/protocol-traits.html#jsonname-trait)
  * [timestampFormat](https://smithy.io/2.0/spec/protocol-traits.html#timestampformat-trait)
  * [sparse](https://smithy.io/2.0/spec/type-refinement-traits.html#sparse-trait)
  * [required](https://smithy.io/2.0/spec/type-refinement-traits.html#required-trait)
  * [default](https://smithy.io/2.0/spec/type-refinement-traits.html#default-value-serialization). It is worth noting that, by default, Smithy4s chooses to not serialise default values when the member is optional.
* [alloy traits](https://github.com/disneystreaming/alloy/blob/main/docs/serialisation/json.md)

## Customizing Json Codecs

`Json.payloadCodecs` provides several methods for customization. A jsoniter `ReaderConfig` and `WriterConfig` can be provided via the
`withJsoniterReaderConfig` and `withJsoniterWriterConfig` methods. Additionally Smithy4s provides further customization through `JsoniterCodecCompiler`.
Below is an example on how to configure the endoer to include `null` for optional fields.

```scala
val personJSON3 = Json.payloadCodecs
  .configureJsoniterCodecCompiler(cfg =>
    cfg.withExplicitDefaultsEncoding(true)
  )
  .encoders
  .fromSchema(Person.schema)
  .encode(person)
  .toUTF8String
```

## Codec Compiler Options

The options available through `JsoniterCodecCompiler` are:

### withMaxArity

**default**: 1024

Changes the behaviour of the decoders so that they fail after a certain number of elements when decoding arrays and maps. 
This allows to protect against some DDOS attacks.

## withFieldFilter

**default**: FieldFilter.Default

Changes the behaviour of Json encoders. Can be used to skip empty collections or unset optional fields to reduce the size of the final json.
The default behavior skips unset optional fields or optional fields that have their value equal to the default value.

### withExplicitDefaultsEncoding (deprecated)

**default**: false

This method is deprecated use `withFieldFilter` instead.

Changes the behaviour of Json encoders so that optional values are encoded as
explicit Json null values.

### withFlexibleCollectionsSupport

**default**: false

Changes the behaviour of Json decoders so that they overlook null values in collections
and maps. This behaviour has a performance overhead.

### withInfinitySupport

**default**: false

Changes the behaviour of Json decoders so that they can parse Infinity/NaN values.
This behaviour has a performance overhead.

### withMapOrderPreservation

**default**: false

Changes the behaviour of Json decoders so that the preserve the ordering of maps.

### withHintMask

Changes the hint mask with which the decoder works. Depending on the hint mask, some
smithy traits may be overlooked during encoding/decoding. For instance, `@jsonName`.

### withLenientTaggedUnionDecoding

Enables lenient decoding of tagged unions, where unset alternatives are encoded as null
values in the json payload. Also ignores unrecognised union keys.

### withLenientNumericDecoding

Enables lenient decoding of numeric values, where numbers may be carried by JSON strings
as well as JSON numbers.
