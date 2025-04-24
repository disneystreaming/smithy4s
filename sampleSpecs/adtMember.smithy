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

// this can reach AdtMixinOne transitively in one of the cases, and directly in the other case
// https://github.com/disneystreaming/smithy4s/issues/1312
@adt
union AdtUnionWithSomeTransitiveMixins {
    s1: AdtMemberWithTransitiveMixin1
    s2: AdtMemberWithDirectMixin
}

@mixin
structure TransitiveMixin with [AdtMixinOne] {}

structure AdtMemberWithTransitiveMixin1 with [TransitiveMixin] {}

structure AdtMemberWithDirectMixin with [AdtMixinOne] {}

// this can reach AdtMixinOne transitively in all cases.
// serves as an example in which both the direct and transitive mixin are added
// to the generated Scala trait's supertypes.
@adt
union AdtUnionWithTransitiveAndDirectMixins {
    s1: AdtMemberWithTransitiveMixin2
    s2: AdtMemberWithTransitiveMixin3
}

structure AdtMemberWithTransitiveMixin2 with [TransitiveMixin] {}
structure AdtMemberWithTransitiveMixin3 with [TransitiveMixin] {}
