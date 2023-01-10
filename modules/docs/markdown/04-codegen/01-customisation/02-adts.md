---
sidebar_label: ADTs
title: Algebraic data types
---

The default behavior of Smithy4s when rendering unions that target structures is to render the structure
in a separate file from the union that targets it. This makes sense if the structure is used in other
contexts other than the union. However, it also causes an extra level of nesting within the union.
This is because the union will create another case class to contain your structure case class.

For example:

```kotlin
union OrderType {
  inStore: InStoreOrder
}

structure InStoreOrder {
    @required
    id: OrderNumber
    locationId: String
}
```

Would render the following scala code:

OrderType.scala:
```scala
sealed trait OrderType extends scala.Product with scala.Serializable
case class InStoreCase(inStore: InStoreOrder) extends OrderType
```

InStoreOrder.scala:
```scala
case class InStoreOrder(id: OrderNumber, locationId: Option[String] = None)
```

The sealed hierarchy `OrderType` has a member named `InStoreCase`. This is because
`InStoreOrder` is rendered in a separate file and `OrderType` is sealed.

However, adding the `adtMember` trait to the `InStoreOrder` structure changes this.

```kotlin
union OrderType {
  inStore: InStoreOrder
}

@adtMember(OrderType) // added the adtMember trait here
structure InStoreOrder {
    @required
    id: OrderNumber
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
This means it must be targeted by the union that is provided as parameter to the adtMember trait.
This constraint is fulfilled above because `OrderType` targets `InStoreOrder` and `InStoreOrder` is
annotated with `@adtMember(OrderType)`.
The structure annotated with `adtMember` (e.g. `InStoreOrder`) also must not be targeted by any other
structures or unions in the model. There is a validator that will make sure these requirements are met
whenever the `adtMember` trait is in use.

Note: The `adtMember` trait has NO impact on the serialization/deserialization behaviors of Smithy4s.
The only thing it changes is what the generated code looks like. This is accomplished by keeping the
rendered schemas equivalent, even if the case class is rendered in a different place.
