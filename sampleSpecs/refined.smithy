namespace smithy4s.example

use smithy4s.meta#refinement
use smithy4s.meta#unwrap

@trait(selector: "integer")
@refinement(
  targetType: "smithy4s.example.refined.Age",
  providerInstance: "smithy4s.example.refined.Age.provider"
)
structure ageFormat {}

@trait(selector: "list")
@refinement(
  targetType: "smithy4s.example.refined.FancyList",
  providerInstance: "smithy4s.example.refined.FancyList.provider"
)
structure fancyListFormat {}

@trait(selector: "string")
@refinement(
  targetType: "smithy4s.example.refined.Name",
  providerInstance: "smithy4s.example.refined.Name.provider"
)
structure nameFormat {}

@ageFormat
integer Age

@ageFormat
integer PersonAge

@fancyListFormat
list FancyList {
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
  name: Name,
  dogName: DogName
}
