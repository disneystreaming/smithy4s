namespace smithy4s.example

use smithy4s.meta#refined

@trait(selector: "integer")
@refined(
  targetClasspath: "smithy4s.example.refined.Age",
  providerClasspath: "smithy4s.example.refined.Age.provider"
)
structure ageFormat {}

@ageFormat
integer Age

@ageFormat
integer PersonAge

structure TestItOut {
  age: Age,
  personAge: PersonAge
}
