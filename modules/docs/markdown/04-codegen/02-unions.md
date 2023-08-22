---
sidebar_label: Unions and sealed traits
title: Unions and sealed traits
---

Smithy's `union` keyword allow to define a co-product, namely a piece of data that can take one form among a list of possibilities.

This concept translates naturally to Scala sealed-traits (or Scala 3 enums), and `union` are therefore generated as such.

```kotlin
union MyUnion {
  i: Integer
  s: MyStructure
  u: Unit
}

structure MyStructure {
  b: Boolean
}
```

Translates to :

```scala
sealed trait MyUnion
object MyUnion {
  case class ACase(a: Int) extends MyUnion
  case class SCase(s: MyStructure) extends MyUnion
  case object UCase extends MyUnion
}
```

As you can see, each member of the sealed-trait ends up generated as a `case class` wrapping the type corresponding to what the union member points to in Smithy.

However, having a union member point to the `Unit` shape in Smithy leads to the corresponding sealed-trait member being generated as a `case object`.

### Flattening of structure members

Under certain conditions, Smithy4s offers a mechanism to "flatten" structure members directly as a member of the sealed trait.

Head over to the page explaining code-gen [customisation](01-customisation/02-adts.md) for a detailed explanation.

### Regarding JSON encoding

Smithy4s does not rely on the classic automated derivation mechanisms to determine how unions should be encoded in JSON. Rather, the Smithy models dictates the encoding. Indeed, there are multiple ways to encode unions in JSON.

By default, the specification of the Smithy language hints that the `tagged-union` encoding should be used. This is arguably the best encoding for unions, as it works with members of any type (not just structures), and does not require backtracking during parsing, which makes it more efficient.

However, Smithy4s provides support for two additional encodings: `discriminated` and `untagged`, which users can opt-in via the `alloy#discriminated` and `alloy#untagged` trait, respectively. These are mostly offered as a way to retrofit existing APIs in Smithy.


#### Tagged union

This is the default behaviour, and happens to visually match how Smithy unions are declared. In this encoding, the union is encoded as a JSON object with a single key-value pair, the key signalling which alternative has been encoded.

```
union Tagged {
  first: String
  second: IntWrapper
}

structure IntWrapper {
  int: Integer
}
```

The following instances of `Tagged`

```scala
Tagged.FirstCase("smithy4s")
Tagged.SecondCase(IntWrapper(42)))
```

are encoded as such :

```json
{ "first": "smithy4s" }
{ "second": { "int": 42 } }
```

#### Untagged union

Untagged unions are supported via an annotation: `@untagged`. Despite the smaller payload size this encoding produces, it is arguably the worst way of encoding unions, as it may require backtracking multiple times on the parsing side. Use this carefully, preferably only when you need to retrofit an existing API into Smithy

```kotlin
use alloy#untagged

@untagged
union Untagged {
  first: String
  second: IntWrapper
}

structure IntWrapper {
  int: Integer
}
```

The following instances of `Untagged`

```scala
Untagged.FirstCase("smithy4s")
Untagged.SecondCase(Two(42)))
```

are encoded as such :

```json
"smithy4s"
{ "int": 42 }
```

#### Discriminated union

Discriminated union are supported via an annotation: `@discriminated("tpe")`, and work only when all members of the union are structures.
In this encoding, the discriminator is inlined as a JSON field within JSON object resulting from the encoding of the member.

Despite the JSON payload exhibiting less nesting than in the `tagged union` encoding, this encoding often leads to bigger payloads, and requires backtracking once during parsing.

```kotlin
use alloy#discriminated

@discriminated("tpe")
union Discriminated {
  first: StringWrapper
  second: IntWrapper
}

structure StringWrapper {
  string: String
}

structure IntWrapper {
  int: Integer
}
```

The following instances of `Discriminated`

```scala
Discriminated.FirstCase(StringWrapper("smithy4s"))
Discriminated.SecondCase(IntWrapper(42)))
```

are  encoded as such

```json
{ "tpe": "first", "string": "smithy4s" }
{ "tpe": "second", "int": 42 }
```

## Union Projections and Visitors

In order to make working with unions more ergonomic, smithy4s provides projection functions and generates visitors for all unions.

#### Projection Functions

Here we will see what a projection function looks like using a simple union example of `Pet`.

```scala
sealed trait Pet {
  object project {
    def dog: Option[Dog]
    def cat: Option[Cat]
  }
}
object Pet {
  case class DogCase(dog: Dog) extends Pet
  case class CatCase(cat: Cat) extends Pet
}
```

These functions can then be used as follows:

```scala
val myPet: Pet = Pet.DogCase(Dog(name = "Spot"))

myPet.project.dog // Some(Dog(name = "Spot"))
myPet.project.cat // None
```

These projection functions make it so you can work with specific union alternatives without needing to do any pattern matching.

#### Visitors

Using the same pet example, we will now see what the visitors look like that smithy4s generates.

```scala
sealed trait Pet {
  def accept[A](visitor: Pet.Visitor[A]): A = // ...
}
object Pet {
  case class DogCase(dog: Dog) extends Pet
  case class CatCase(cat: Cat) extends Pet

  trait Visitor[A] {
    def dog(dog: Dog): A
    def cat(cat: Cat): A
  }
}
```

Similar to the projection functions, the visitor allows us to handle the alternatives without a pattern match. For example:

```scala
val myPet: Pet = Pet.DogCase(Dog(name = "Spot"))

val visitor = new Pet.Visitor[String] {
    def dog(dog: Dog): String = s"Dog named ${dog.name}"
    def cat(cat: Cat): String = s"Cat named ${cat.name}"
}

myPet.accept(visitor) // "Dog named Spot"
```

You can also implement a Visitor using `Visitor.Default` to provide a default value to be used for cases that you don't explicitly implement. For example:

```scala
val myPet: Pet = Pet.DogCase(Dog(name = "Spot"))

val visitor = new Pet.Visitor.Default[String] {
    def default: String = "default value"
    def cat(cat: Cat): String = s"Cat named ${cat.name}"
}

myPet.accept(visitor) // "default value"
```
