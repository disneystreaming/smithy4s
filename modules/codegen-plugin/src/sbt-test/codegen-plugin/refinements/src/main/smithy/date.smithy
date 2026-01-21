$version: "2.0"

namespace scripted.date


use smithy4s.meta#refinement

apply dateFormat @refinement(
  targetType: "java.time.LocalDate",
  providerImport: "refinements.validated.Refinements.dateFormat.provider._"
)

@trait(selector:"*")
structure dateFormat {
}
structure StructWithDate {
  @dateFormat
  @required
  date: String,
}
