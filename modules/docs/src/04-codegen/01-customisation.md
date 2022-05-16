---
sidebar_label: Protocols and smithy4s
title: Protocols and smithy4s
---

Smithy4s is opinionated in what the generated code look like, there are a few things that can be tweaked.

#### Packed inputs

By default, smithy4s generates methods the parameters of which map to the fields of the input structure of the corresponding operation.

For instance :

```kotlin
service PackedInputsService {
  version: "1.0.0",
  operations: [PackedInputOperation]
}

operation PackedInputOperation {
  input: PackedInput,
}

structure PackedInput {
    @required
    a: String,
    @required
    b: String
}
```

leads to something conceptually equivalent to :

```scala
trait PackedInputServiceGen[F[_]] {

  def packedInputOperation(a: String, b: String) : F[Unit]

}
```

It is however possible to annotate the service (or operation) definition with the `smithy4s.meta#packedInputs` trait, in order for the rendered method to contain a single parameter, typed with actual input case class of the operation.

For instance :

```scala
use smithy4s.meta#packedInputs

@packedInputs
service PackedInputsService {
  version: "1.0.0",
  operations: [PackedInputOperation]
}
```

will produce the following Scala code

```scala  
trait PackedInputServiceGen[F[_]] {

  def packedInputOperation(input: PackedInput) : F[Unit]

}
```

#### ADT Member Trait

The default behavior of smithy4s when rendering unions that target structures is to render the structure as a normal
ADT sealed trait hierarchy and create a `MyStructureCase` class that extends the union's sealed trait and takes
`MyStructure` as a parameter.

For example:

```kotlin
union OrderType {
  inStore: InStoreOrder
}

structure InStoreOrder {
    @required
    id: OrderNumber,
    locationId: String
}
```

Would render the following scala code:

```scala
sealed trait OrderType extends scala.Product with scala.Serializable
case class InStoreCase(inStore: InStoreOrder) extends OrderType
```

However, adding the `adtMember` trait to the `InStoreOrder` structure changes this.

```kotlin
union OrderType {
  inStore: InStoreOrder
}

@adtMember(OrderType)
structure InStoreOrder {
    @required
    id: OrderNumber,
    locationId: String
}
```

```scala
sealed trait OrderType extends scala.Product with scala.Serializable
case class InStoreOrder(id: OrderNumber, locationId: Option[String] = None) extends OrderType
```

The `IsStoreOrder` class has now been updated to be rendered directly as a member of the `OrderType`
sealed hierarchy.

*The `adtMember` trait can be applied to any structure as long as said structure is targeted by EXACTLY ONE union.*
This means it must be targeted by the union that is provided as parameter to the trait (e.g. `MyUnion` above).
The structure also must not be targeted by any other structures or unions in the model. There is a validator
that will make sure these requirements are met whenever this trait is in use.

Note: The `adtMember` trait has NO impact on the serialization/deserialization behaviors of smithy4s.
The only thing it changes is what the generated code looks like.
