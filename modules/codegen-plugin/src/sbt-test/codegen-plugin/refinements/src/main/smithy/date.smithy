$version: "2.0"

namespace scripted.date

use alloy#dateFormat
use smithy4s.meta#refinement

apply alloy#dateFormat @refinement(
  targetType: "java.time.LocalDate",
  providerImport: "refinements.validated.Refinements.dateFormat.provider._"
)

structure StructWithDate {
  @dateFormat
  @required
  date: String,
}