---
sidebar_label: ADTs
title: Algebraic data types
---

The default behavior of Smithy4s when rendering unions that target structures is to render the structure
in a separate file from the union that targets it. This makes sense if the structure is used in other
contexts other than the union. However, it also causes an extra level of nesting within the union.
This is because the union will create another case class to contain your structure case class.

For example:

```smithy
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

## smithy4s.meta#adt Trait

Adding the `smithy4s.meta#adt` trait to a `OrderType` union changes how the code for that union is generated.

```smithy
@adt // added the adt trait here
union OrderType {
  inStore: InStoreOrder
}

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
sealed hierarchy instead of in its own file.

#### Restrictions and Validation

Using the `adt` trait does come with some restrictions. First are requirements for the union which is annotated with the `adt` trait:

- The union must contain at least one member
- The union's members must only target structure shapes

Additionally, there is a requirement that is added onto the structure shapes that the union targets:

- The structures must NOT be the target of any other union, structure, etc. They can only be the target in the ONE union that is annotated with the `adt` trait.

A validator will be run automatically on your model to make sure it conforms to the requirements above.

Note: The `adt` trait has NO impact on the serialization/deserialization behaviors of Smithy4s.
The only thing it changes is what the generated code looks like. This is accomplished by keeping the
rendered schemas equivalent, even if the case class is rendered in a different place.

#### Mixins

The `adt` trait has some extra functionality in place to improve ergonomics when working with the generated code. Specifically, the smithy4s code generation will extract all common mixins from the structure members the union targets and move them to the level of the sealed trait that represents the adt. This is easier to conceptualize with an example:

```smithy
@adt
union OrderType {
  inStore: InStoreOrder
  online: OnlineOrder
}

@mixin
structure HasId {
  @required
  id: String
}

@mixin
structure HasLocation {
  @required
  locationId: String
}

structure InStoreOrder with [HasId, HasLocation] {
    description: String
}

structure OnlineOrder with [HasId] {
  @required
  userId: String
}
```

This Smithy model will lead to the following generated code:

HasId.scala:
```scala
trait HasId {
  def id: String
}
```

HasLocation.scala:
```scala
trait HasLocation {
  def locationId: String
}
```

```scala
sealed trait OrderType extends HasId scala.Product with scala.Serializable
case class InStoreOrder(id: String, locationId: String) extends OrderType with HasLocation
case class OnlineOrder(id: String, userId: String) extends OrderType
```

Since both `OnlineOrder` and `InStoreOrder` use the `HasId` mixin, that mixin is moved to the `OrderType` level in the generated code. This allows for more flexibility when working with adts. Since only `InStoreOrder` uses the `HasLocation` mixin, that mixin is kept at the level of `InStoreOrder` and extended directly by that case class. 

## smithy4s.meta#adtMember Trait

Below we will explore the `smithy4s.meta#adtMember` trait. This trait is mutually exclusive from the `adt` trait described above. It has essentially the same effect as the `adt` trait, with the exception that it DOES NOT extract common mixins to the sealed trait level like the `adt` trait does.

Here is an example of using the `adtMember` trait:

```smithy
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
