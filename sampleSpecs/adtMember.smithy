$version: "2.0"

namespace smithy4s.example

use smithy4s.meta#adtMember
use smithy4s.meta#adt

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

@adt
union TestAdt {
  one: AdtOne
  two: AdtTwo
}

@mixin
structure AdtMixinOne {
  lng: Long
}

@mixin
structure AdtMixinTwo {
  sht: Short
}

@mixin
structure AdtMixinThree {
  blb: Blob
}

structure AdtOne with [AdtMixinOne, AdtMixinTwo, AdtMixinThree] {
  str: String
}

structure AdtTwo with [AdtMixinOne, AdtMixinTwo] {
  int: Integer
}

@adt
union Podcast {
  video: Video
  audio: Audio
}

@mixin
structure PodcastCommon {
  title: String
  url: String
  durationMillis: Long
}

structure Video with [PodcastCommon] {}
structure Audio with [PodcastCommon] {}
