---
sidebar_label: URL Form
title: URL Form serialisation
---

The `smithy4s-core` module provides a `smithy4s.http.UrlForm` data-type, along with encoders and decoders that can read/write generated data-types from/to the `application/x-www-form-urlencoded` format. This serialisation is what powers the [AWS query](https://smithy.io/2.0/aws/protocols/aws-query-protocol.html) protocol implementation in Smithy4s.

`UrlForm` is a case class wrapping a list of key/value pairs. It can be rendered to a `String` via `.render`, and parsed back with `UrlForm.parse`.

```scala mdoc:reset
import smithy4s.example.hello.Person

import smithy4s.http.UrlForm

val personEncoder = UrlForm
  .Encoder(capitalizeStructAndUnionMemberNames = false)
  .fromSchema(Person.schema)

val personUrlForm = personEncoder.encode(Person(name = "John Doe"))
val personString = personUrlForm.render

val personDecoder = UrlForm
  .Decoder(
    ignoreUrlFormFlattened = false,
    capitalizeStructAndUnionMemberNames = false
  )
  .fromSchema(Person.schema)

val maybePerson = UrlForm.parse(personString).flatMap(personDecoder.decode)
```

## Configuration

Both `UrlForm.Encoder` and `UrlForm.Decoder` take a `capitalizeStructAndUnionMemberNames` flag. When `true`, member names are capitalised in the output (and expected to be capitalised on input) - this matches the convention used by the AWS query protocol.

`UrlForm.Decoder` additionally accepts an `ignoreUrlFormFlattened` flag, which disables special handling of the `@alloy#urlFormFlattened` trait when decoding.

`UrlForm.Encoder` has an overload with an `alwaysSkipEmptyLists` flag, which omits empty lists from the encoded output regardless of any other configuration.

## Supported traits

By default, `UrlForm` codecs honour the following [alloy traits](https://github.com/disneystreaming/alloy):

* `@alloy#urlFormName` - changes the serialised key of a structure, union, or member. This is analogous to `@jsonName` for JSON serialisation.
* `@alloy#urlFormFlattened` - unwraps the values of a list, set, or map into the containing structure or union, rather than nesting them under the member's key.

For example, given the following Smithy definition:

```smithy
use alloy#urlFormName
use alloy#urlFormFlattened

structure Order {
    @urlFormName("order-id")
    @required
    id: String

    @urlFormFlattened
    items: ItemList
}

list ItemList {
    member: String
}
```

an `Order("123", List("a", "b"))` value would encode to `order-id=123&items.1=a&items.2=b`, with the list members flattened directly under the `items` key.
