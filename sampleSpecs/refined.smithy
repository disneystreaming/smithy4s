namespace smithy4s.example

use smithy4s.meta#refined

@trait(selector: "integer")
@refined(
  targetClasspath: "smithy4s.example.refined.Age",
  providerClasspath: "smithy4s.example.refined.Age.provider"
)
structure ageFormat {}

@trait(selector: "list")
@refined(
  targetClasspath: "smithy4s.example.refined.FancyList",
  providerClasspath: "smithy4s.example.refined.FancyList.provider"
)
structure fancyListFormat {}

@trait(selector: "string")
@refined(
  targetClasspath: "smithy4s.example.refined.Name",
  providerClasspath: "smithy4s.example.refined.Name.provider"
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

structure TestItOut {
  age: Age,
  personAge: PersonAge,
  fancyList: FancyList,
  name: Name
}
