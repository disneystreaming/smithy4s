namespace smithy4s.example

use smithy4s.meta#adtMember

integer OrderNumber

/// Our order types have different ways to identify a product
/// Except for preview orders, these don't have an ID 
union OrderType {
  online: OrderNumber,
  /// For an InStoreOrder a location ID isn't needed
  inStore: InStoreOrder,
  preview: Unit
}

@adtMember(OrderType)
structure InStoreOrder {
    @required
    id: OrderNumber,
    locationId: String
}

@trait
structure testTrait {
  orderType: OrderType
}

@testTrait(orderType: {
  inStore: {
    id: 100,
    locationId: "someLocation"
  }
})
string TestString
