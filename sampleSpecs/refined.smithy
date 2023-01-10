namespace smithy4s.example

use smithy4s.meta#refinement
use smithy4s.meta#unwrap

@trait(selector: "integer")
@refinement(
  targetType: "smithy4s.example.refined.Age",
  providerImport: "smithy4s.example.refined.Age.provider._"
)
structure ageFormat {}

@trait(selector: "list:test(> member > string)") // lists with string members
@refinement(
  targetType: "smithy4s.example.refined.FancyList"
)
structure fancyListFormat {}

@trait(selector: "string")
@refinement(
  targetType: "smithy4s.example.refined.Name"
)
structure nameFormat {}

@trait(selector: "list")
@refinement(
  targetType: "smithy4s.example.refined.NonEmptyList",
  parameterised: true
)
structure nonEmptyListFormat {}

@trait(selector: "map")
@refinement(
  targetType: "smithy4s.example.refined.NonEmptyMap",
  parameterised: true
)
structure nonEmptyMapFormat {}

@nonEmptyListFormat
list NonEmptyStrings {
  member: String
}

@nonEmptyListFormat
list NonEmptyNames {
  member: Name
}

structure Candy {
  name: String
}

@nonEmptyListFormat
list NonEmptyCandies {
  member: Candy
}

@nonEmptyMapFormat
map NonEmptyMapNumbers {
  key: String
  value: Integer
}

@ageFormat
integer Age

@ageFormat
integer PersonAge

@fancyListFormat
list FancyList {
  member: String
}

@fancyListFormat
@unwrap
list UnwrappedFancyList {
  member: String
}

@nameFormat
string Name

@nameFormat
@unwrap
string DogName

structure StructureWithRefinedTypes {
  age: Age,
  personAge: PersonAge,
  fancyList: FancyList,
  unwrappedFancyList: UnwrappedFancyList,
  name: Name,
  dogName: DogName
}

union UnionWithRefinedTypes {
  age: Age,
  dogName: DogName
}
