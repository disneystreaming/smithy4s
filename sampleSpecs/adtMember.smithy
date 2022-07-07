namespace smithy4s.example

use smithy4s.meta#adtMember

integer OrderNumber

union OrderType {
  online: OrderNumber,
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
