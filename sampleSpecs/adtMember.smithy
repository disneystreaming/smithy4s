namespace smithy4s.example

use smithy4s.meta#adtMember

integer OrderNumber

union OrderType {
  online: OrderNumber,
  inStore: InStoreOrder
}

@adtMember(OrderType)
structure InStoreOrder {
    @required
    id: OrderNumber,
    locationId: String
}

@trait
structure TestTrait {
  orderType: OrderType
}
