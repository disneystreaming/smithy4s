$version: "2.0"

namespace smithy4s.example

use smithy4s.meta#adtMember
use smithy4s.meta#adt
use smithy4s.meta#generateOptics

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
@generateOptics
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

@generateOptics
structure Video with [PodcastCommon] {}
@generateOptics
structure Audio with [PodcastCommon] {}


@mixin
structure HasName {
    name: String
}

structure OtherPerson with [HasName] {
    @required
    $name
}

@adt
union PersonUnion {
    p: OtherPerson
}

// https://github.com/disneystreaming/smithy4s/issues/1312
@adt
union Items {
    s1: Item1
    s2: Item2
}

@mixin
structure HasHasStuff with [HasStuff] {}

@mixin
structure HasStuff {
    stuff: String
}

structure Item1 with [HasHasStuff] {}

structure Item2 with [HasStuff] {}
